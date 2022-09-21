package app.peer;

import java.io.IOException;
import java.net.InetAddress;

import app.resource_manager.ResourceManager;
import app.socket.Socket;
import app.socket.Socket.SocketPayload;

public class PeerThread extends Thread {

	protected Socket socket;
	protected int server_port;
	protected InetAddress addr = null;
	protected ResourceManager resourceManager;

	public PeerThread(String[] args, Socket socket, ResourceManager resourceManager) throws IOException {
		server_port = Integer.parseInt(args[3]);
		addr = InetAddress.getByName(args[0]);
		this.socket = socket;
		this.resourceManager = resourceManager;
		String content = args[1];
		socket.sendPacket(content, addr, server_port);
	}

	public void run() {
		while (true) {
			try {
				SocketPayload socketPayload = socket.receivePacket();
				String vars[] = socketPayload.getContent().split("\\s");

				System.out.println("Received: " + String.join(" ", vars));

				if (vars[0].equals("resource") && vars.length > 1) {
					String hash = vars[1];

					if (this.resourceManager.sameHash(hash))
						System.out.println("I HAVE THIS CONTENT! " + this.resourceManager.getContent());
					else
						System.out.println("I DO NOT HAVE THIS CONTENT.");
				}

			} catch (Exception e) {
			}

		}

	}
}