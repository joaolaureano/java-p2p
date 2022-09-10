package peer;
import java.io.*;
import java.net.*;
import java.util.*;

public class p2pPeerThread extends Thread {
	protected DatagramSocket socket = null;
	protected DatagramPacket packet = null;
	protected InetAddress addr = null;
	protected byte[] resource = new byte[1024];
	protected byte[] response = new byte[1024];
	protected int port;
	protected String[] vars;

	public p2pPeerThread(String[] args) throws IOException {
		// envia um packet
		resource = args[1].getBytes();
		addr = InetAddress.getByName(args[0]);
		port = Integer.parseInt(args[2]);
		// cria um socket datagrama
		socket = new DatagramSocket(port);
		vars = args[1].split("\\s");
	}

	public void run() {
		
		try {
			// envia um packet
			DatagramPacket packet = new DatagramPacket(resource, resource.length, addr, 9000);
			socket.send(packet);
		} catch (IOException e) {
			socket.close();
		}
		
		while (true) {
			try {
				// obtem a resposta
				packet = new DatagramPacket(response, response.length);
				socket.setSoTimeout(500);
				socket.receive(packet);
				
				// mostra a resposta
				String data = new String(packet.getData(), 0, packet.getLength());
				System.out.println("recebido: " + data);
				
			} catch (IOException e) {
//				if (!vars[0].equals("wait")) {
//					// fecha o socket
//					socket.close();
//					break;
//				}
			}
		}

	}
}
