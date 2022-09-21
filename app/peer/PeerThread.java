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

					if (this.resourceManager.sameHash(hash)) {
						String succesMessage = "I HAVE THIS CONTENT!";

						System.out.println(succesMessage);
						addr = socketPayload.getAddress();
						int requesterPort = socketPayload.getPort();
						
						socket.sendPacket(succesMessage, addr, requesterPort);
						socket.sendPacket(this.resourceManager.getContent(), addr, requesterPort);
					} else{
						String failureMessage = "I DO NOT HAVE THIS CONTENT...";
						
						System.out.println(failureMessage);

						addr = socketPayload.getAddress();
						int requesterPort = socketPayload.getPort();

						socket.sendPacket(failureMessage, addr, requesterPort);


						}
				}

			} catch (Exception e) {
			}

		}

	}
}