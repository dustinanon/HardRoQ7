package hardroq.networking;

import hardroq.controllers.HivemindController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Random;

public class IRCConnection {
	private static final Random random = new Random();
	private final String SERVER;
	private final String NICK;
	private final String CHANNEL;
	private final Thread IRC_THREAD;
	
	private Socket socket;
	private BufferedWriter outWriter;
	private BufferedReader inReader;
	private int nickSuffix = 0;
	
	public IRCConnection(final String server, final String nick, final String channel) throws Exception {
		SERVER = server;
		NICK = nick;
		CHANNEL = channel;
		
		IRC_THREAD = new Thread(InitiateLogon, "IRC Thread");
		IRC_THREAD.start();
	}


	private Runnable InitiateLogon = new Runnable() {
		@Override
		public void run() {
			try {
				socket = new Socket(SERVER, 6667);
				outWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
			final String realnick = getRealNick();
			
			try {
				//let's go ahead and write the logon information... I'm sure it's sent the motd and shit
				
				send("PASS " + genRandomPass() + "\r\n");
				send("NICK " + realnick + "\r\n");
				send("USER " + realnick + " 0 * :" + realnick);
				
				//Read all this input this until we know we've connected.
				String l;
				while ((l = inReader.readLine()) != null) {
					System.out.println(l);
					if (l.contains("004"))
						break;
					else if (l.contains("433"))
						//this nick is taken, fuck.
						nickSuffix++;
						break;
				}
				
				//I guess we're in, let's join the hivemind chan
				send("JOIN " + CHANNEL);
				
				//let's assume we're in the channel, start processing commands.
				listenToTheHive();
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}

		private void send(final String string) throws Exception {
			System.out.println(string);
			outWriter.write(string);
			outWriter.flush();
		}

		private void listenToTheHive() {
			String in;
			while(true)
			{
				try {
					//first, let's grab any information that's come from the server
					while ((in = inReader.readLine()) != null)
					{
						System.out.println(in);
						//is this a ping?
						if (in.startsWith("PING")) {
							send(in.replace("PING", "PONG"));
							break;
						} else if (in.startsWith("PRIVMSG")) {
							//ooh, somebody is saying something... is it important?
							parseCommand(in);
						}
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}

		private void parseCommand(final String in) {
			//first, let's cut away all the crap.
			final String command = in.substring(in.indexOf(":", 0));
			
			//alright, we have the command, now let's parse it.
			HivemindController.getInstance().parseCommand(command);
		}

		private String getRealNick() {
			return NICK + (nickSuffix > 0 ? String.valueOf(nickSuffix) : "");
		}
		
		private String genRandomPass() {
			StringBuilder b = new StringBuilder();
			for (int i = random.nextInt(8) + 8; --i > 0;)
				switch(random.nextInt(3))
				{
				case 0:
					b.append((char) random.nextInt(10) + 48);
				case 1:
					b.append((char) random.nextInt(26) + 65);
				case 2:
					b.append((char) random.nextInt(26) + 97);
				}
			
			return b.toString();
		}
	};
}
