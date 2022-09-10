package peer;
import java.io.*;
import java.net.*;
import java.util.*;

public class p2pPeerClient extends Thread {
	protected DatagramSocket socket = null;
	protected DatagramPacket packet = null;
	protected InetAddress addr = null;
	protected byte[] resource = new byte[1024];
	protected byte[] response = new byte[1024];
	protected int port, peer_port;

	public p2pPeerClient(String[] args) throws IOException {
		port = Integer.parseInt(args[2]) + 101;
		// cria um socket datagrama
		socket = new DatagramSocket(port);
	}

	public void run() {
		BufferedReader obj = new BufferedReader(new InputStreamReader(System.in));
		String str = "";	

		while (true) {

			System.out.println("\n<list/peer> <message> <ip>");
			System.out.println("Example: list user <server_ip>");
			System.out.println("Example: peer \"hello_world!\" <peer_ip> <port>");
			try {
				str = obj.readLine();
				String vars[] = str.split("\\s");
				addr = InetAddress.getByName(vars[2]);
				String str2 = vars[0] + " " + vars[1];
				resource = str2.getBytes();
				if (vars.length == 4) {
					System.out.println("Sending message to peer on port " + vars[3]);
					peer_port = Integer.parseInt(vars[3]);
				} else {
					peer_port = 9000;
				}
			} catch (IOException e) {
			}
			
			try {
				packet = new DatagramPacket(resource, resource.length, addr, peer_port);
				socket.send(packet);
				
				while (true) {
					try {
						// obtem a resposta
						packet = new DatagramPacket(response, response.length);
						socket.setSoTimeout(500);
						socket.receive(packet);
						
						// mostra a resposta
						String resposta = new String(packet.getData(), 0, packet.getLength());
						System.out.println("recebido: " + resposta);
					} catch (IOException e) {
						break;
					}
				}
			} catch (IOException e) {
			}
		}
	}
}
