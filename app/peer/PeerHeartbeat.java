package app.peer;

import java.io.IOException;
import java.net.InetAddress;

import app.socket.Socket;

public class PeerHeartbeat extends Thread{

    protected Socket socket;
    protected String data = "";
	protected int port;
	protected int server_port;
    protected InetAddress addr = null;

    
    public PeerHeartbeat(String[] args) throws IOException{
        String vars[] = args[1].split("\\s");
        this.data = ("heartbeat " + vars[1]);
		addr = InetAddress.getByName(args[0]);
		
        port = Integer.parseInt(args[2]) + 100;

		server_port = Integer.parseInt(args[3]);
		
		System.out.println("HEART_BEAT_PORT -> " + port);
		System.out.println("SERVER_PORT -> " + server_port);
		System.out.println("USER NAME -> " + vars[1]);

		// cria um socket datagrama
		this.socket = new Socket(port + 100);
    }

    public void run(){
		while (true) {
			try {
				this.socket.sendPacket(data, addr, server_port);
			} catch (Exception e) {
				socket.close();
			}
			
			try {
				Thread.sleep(5000);
			} catch(InterruptedException e) {
			}
		}
    }
}
