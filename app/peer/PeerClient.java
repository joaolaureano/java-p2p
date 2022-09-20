package app.peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import app.socket.Socket;

public class PeerClient extends Thread{
    protected Socket socket;

    public PeerClient(String[] args) throws IOException {
        int port = Integer.parseInt(args[2]) + 101;
        socket = new Socket(port);
    }
 
    public void run(){
        BufferedReader obj = new BufferedReader(new InputStreamReader(System.in));
        String str = "";

        while(true){
            int peerPort = 9000;
            InetAddress addr = null;
            String resource = "";

            System.out.println("\n<list/peer> <message> <ip>");
			System.out.println("Example: list user <server_ip>");
			System.out.println("Example: peer \"hello_world!\" <peer_ip> <port>");

            try{
                str = obj.readLine();
                System.out.println("PEGANDO O STR " + str);
				String vars[] = str.split("\\s");
				addr = InetAddress.getByName(vars[vars.length - 2]);
                String str2 = "" ;
                for(int i = 0; i < vars.length - 2; i++){
                    str2 += " " + vars[i];
                }
				resource = str2.trim();
            }
            catch(IOException e){
                System.out.println(e.getMessage());
            }

            try{
            System.out.println("Sending packet");
            System.out.println(resource);
            System.out.println(addr.toString());
            System.out.println(peerPort);
                this.socket.sendPacket(resource, addr, peerPort);
                while(true){
                    try {
                        String socketResponse = this.socket.receivePacket().getContent();
                        System.out.println("Received from server -> " + socketResponse);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            catch(Exception e){
                // System.out.println(e.getMessage());
            }

        }
    }
}