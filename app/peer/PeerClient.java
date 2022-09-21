package app.peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import app.resource_manager.ResourceManager;
import app.socket.Socket;

public class PeerClient extends Thread {
    protected Socket socket;
    protected ResourceManager resourceManager;

    public PeerClient(String[] args, Socket socket, ResourceManager resourceManager) throws IOException {
        this.socket = socket;
        this.resourceManager = resourceManager;
    }

    public void run() {
        BufferedReader obj = new BufferedReader(new InputStreamReader(System.in));
        String str = "";
        
        while (true) {
            int destinyPort = 0;
            InetAddress addr = null;
            String resource = "";
            System.out.println("\nAvailable commands -> <list> | <peer>");
            System.out.println("Example: list user <server_ip> <server_port>");
            System.out.println("Example: peer \"hello_world!\" <peer_ip> <peer_port>");
            System.out.println("Example: register <peer_ip> <peer_port>");
            System.out.println("Example: resource <hash> <peer_ip> <peer_port>");

            try {
                str = obj.readLine();
                String vars[] = str.split("\\s");
                
                addr = InetAddress.getByName(vars[vars.length - 2]);
                destinyPort = Integer.parseInt(vars[vars.length - 1]);
                
                if(vars[0].equals("register")){
                    resource = "register" + " " + this.resourceManager.getHash();
                    this.socket.sendPacket(resource, addr, destinyPort);
                }
                if(vars[0].equals("resource")){
                    resource = "resource" + " " + vars[1];
                    this.socket.sendPacket(resource, addr, destinyPort);
                }
                if(vars[0].equals("list")){
                    resource = "list" + " " + vars[1];
                    this.socket.sendPacket(resource, addr, destinyPort);
                }
                
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            

        }
    }
}
