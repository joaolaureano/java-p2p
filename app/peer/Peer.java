package app.peer;

import java.io.IOException;

public class Peer {

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Uso: java p2pPeer <server> \"<message>\" <localport>");
			System.out.println("<message> is:");
			System.out.println("create nickname");
			return;
		} else {
			new PeerThread(args).start();
			new PeerHeartbeat(args).start();
			new PeerClient(args).start();
		}
	}
}