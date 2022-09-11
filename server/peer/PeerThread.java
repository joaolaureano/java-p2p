package server.peer;

import java.io.IOException;
import java.net.InetAddress;

import server.Socket;
import server.Socket.SocketPayload;

public class PeerThread  extends Thread{

    protected Socket socket;
	protected String content;
	protected InetAddress addr = null;

	public PeerThread(String[] args) throws IOException{

		int port = Integer.parseInt(args[2]);
		addr = InetAddress.getByName(args[0]);
		socket = new Socket(port);
		content = args[1];
		
	}

	public void run(){
		try {
			socket.sendPacket(content, addr, 9000);
		} catch (Exception e) {
			socket.close();
		}

		while(true){

			try {
				String receivedPacket = socket.receivePacket().getContent();
				System.out.println("Received: " + receivedPacket);

			} catch (Exception e) {
			}

		}

	}
}