package app.peer;

import java.io.IOException;
import java.net.InetAddress;

import app.socket.Socket;

public class PeerThread  extends Thread{

    protected Socket socket;
	protected String content;
	protected int server_port;
	protected InetAddress addr = null;


	public PeerThread(String[] args) throws IOException{

		int port = Integer.parseInt(args[2]);
		server_port = Integer.parseInt(args[3]);
		addr = InetAddress.getByName(args[0]);
		socket = new Socket(port);
		content = args[1];
		
	}

	public void run(){
		try {
			socket.sendPacket(content, addr, server_port);
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