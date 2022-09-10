package peer;
import java.io.*;
import java.net.*;
import java.util.*;

public class p2pPeer {

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Uso: java p2pPeer <server> \"<message>\" <localport>");
			System.out.println("<message> is:");
			System.out.println("create nickname");
			System.out.println("list nickname");
			return;
		} else {
			new p2pPeerThread(args).start();
			new p2pPeerHeartbeat(args).start();
			new p2pPeerClient(args).start();
		}
	}
}
