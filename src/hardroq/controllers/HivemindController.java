package hardroq.controllers;

import hardroq.networking.IRCConnection;

public class HivemindController {
	private static HivemindController _instance = new HivemindController();
	private HivemindController() {}
	
	//internals
	private String server;
	private String nick;
	private String channel;
	private IRCConnection bot;
	
	public static HivemindController getInstance() {
		return _instance;
	}
	
	public void connectToHivemind() {
		try {
			bot = new IRCConnection(server, nick, channel);
		} catch (Exception e) {}
	}
	
	public void setServer(final String s) {
		server = s;
	}
	
	public void setNick(final String n) {
		nick = n;
	}
	
	public void setChannel(final String c) {
		channel = c;
	}
	
	public void parseCommand(final String command) {
		if (command.startsWith("!"))
		{
			int spPos = command.indexOf(" ");
			final String c = command.substring(0, spPos).toLowerCase();
			final String[] vars = command.substring(spPos).split(" ");
			
			if (c == "!attack") {
				//we have an attack command
				//let's continue to parse the rest of the string to figure out our variables
				for (int i = 0; i < vars.length; i++)
				{
					if (vars[i] == "--target")
						AttackController.getInstance().setTarget(vars[++i]);
					else if (vars[i] == "--port")
						AttackController.getInstance().setPort(Integer.parseInt(vars[++i]));
				}
				
				//ATTACK!
				AttackController.getInstance().Attack();
			} else if (c == "!settarget")
				AttackController.getInstance().setTarget(vars[0]);
			else if (c == "!setport")
				AttackController.getInstance().setPort(Integer.parseInt(vars[1]));
		}
	}
}
