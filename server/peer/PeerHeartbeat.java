package server.peer;

import java.io.IOException;
import java.net.InetAddress;

import server.Socket;

public class PeerHeartbeat extends Thread{

    protected Socket socket;
    protected String data = "";
    protected InetAddress addr = null;

    
    public PeerHeartbeat(String[] args) throws IOException{
        String vars[] = args[1].split("\\s");
        this.data = ("heartbeat " + vars[1]);
		addr = InetAddress.getByName(args[0]);
		
        int porta = Integer.parseInt(args[2]) + 100;
		// cria um socket datagrama
		this.socket = new Socket(porta);
    }

    public void run(){
		while (true) {
			try {
				this.socket.sendPacket(data, addr, 9000);
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
