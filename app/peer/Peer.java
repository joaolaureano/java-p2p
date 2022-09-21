package app.peer;

import java.io.IOException;

import app.resource_manager.ResourceManager;
import app.socket.Socket;

public class Peer {

	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			System.out.println("Uso: java p2pPeer <server> \"<message>\" <peer_port> <server_port>");
			System.out.println("<message> is:");
			System.out.println("create <nickname>");
			return;
		} else {
			System.out.println(Integer.parseInt(args[2]));
			Socket newSocket = new Socket(Integer.parseInt(args[2]));
			ResourceManager resourceManager = new ResourceManager(args[0]);
			System.out.println("Resouce is " + args[0]);
			new PeerThread(args, newSocket, resourceManager).start();
			new PeerClient(args, newSocket, resourceManager).start();
			new PeerHeartbeat(args).start();
		}
	}
}