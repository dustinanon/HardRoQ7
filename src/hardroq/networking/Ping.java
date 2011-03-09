package hardroq.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Ping {
	//Constants
	public static final String httpPingPacket = "GET / HTTP/1.0\r\n\r\n";
	
	//fields
	public static Socket socket;
	public static BufferedReader in;
	public static OutputStream out;
	public static PrintWriter printer;
	
	
	public static int HTTPPing(String host, int port) throws IOException {
		socket = new Socket(host, port);
		
		out = socket.getOutputStream();
		printer = new PrintWriter(out);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		long start;
		long RTT = 0;
		
		start = System.currentTimeMillis();
		printer.write(httpPingPacket);
		printer.flush();
		
		//final byte[] buffer = new byte[1];
		final String line = in.readLine();
		RTT = System.currentTimeMillis() - start;
		
		return (int)RTT;
	}
}
