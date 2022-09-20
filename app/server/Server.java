package app.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.bucket.DHTBucket;
import app.bucket.IDHTBucket;
import app.command.CreateCommand;
import app.command.DecreaseHeartBeat;
import app.command.HeartBeatCommand;
import app.command.ICommand;
import app.command.ListCommand;
import app.command.RegisterCommand;
import app.socket.Socket;
import app.socket.Socket.SocketPayload;

public class Server {
    static Socket socket;
    static int port;

    static HashMap<String, Integer> timeout = new HashMap<String, Integer>();
    static List<HostData> hostList = new ArrayList<>();
    static IDHTBucket<String> bucket;
    static Server next_sp;
    // NEED TO CREATE A CONSTRUCTOR TO DEFINE BUCKET POSITION AND SIZE

    // public Server(int numberOfNodes, int position, int port, Server next_sp){
    //     this.next_sp = next_sp;
    //     this.port = port;
    //     this.bucket = new DHTBucket<String>(8, 1);
    // }

    public static void main(String[] args) throws IOException {
        System.out.println("OPEN");
        System.out.println("port is " + args[0]);
        System.out.println("next port is " +args[1]);
        System.out.println("ring position is " + args[2]);
        // socket = new Socket(port);
        // while (true) {
            // try {
                // SocketPayload socketPayload = socket.receivePacket();
// 
                // String vars[] = socketPayload.getContent().split("\\s");
                // InetAddress address = socketPayload.getAddress();
                // int port = socketPayload.getPort();
                // String hostName = vars[1];
// 
                // if (vars[0].equals("create") && vars.length > 1) {
                    // HostData hostData = new HostData(address, hostName, port);
                    // ICommand<Boolean> command = new CreateCommand(hostList, timeout, hostData);
                    // boolean commandResult = command.run();
// 
                    // String response = commandResult ? "OK" : "NOT OK";
// 
                    // socket.sendPacket(response, address, port);
                // }
                // if (vars[0].equals("heartbeat") && vars.length > 1) {
                    // HostData hostData = new HostData(address, hostName, port);
                    // ICommand<Boolean> command = new HeartBeatCommand(timeout, hostData);
                    // command.run();
// 
                // }
                // if (vars[0].equals("list") && vars.length > 1) {
// 
                    // ICommand<String> command = new ListCommand(hostList);
                    // String[] response = command.run().split("\r?\n|\r");
// 
                    // for (String responseEntry : response) {
                        // socket.sendPacket(responseEntry, address, port);
                    // }
            //     }
            //     if (vars[0].equals("register") && vars.length > 1) {
                    
            //         String hash = vars[2];

            //         ICommand<Boolean> command = new RegisterCommand(bucket, hash, "DUMB DATA");

            //         boolean response = command.run();

            //         System.out.println("RETURNED " + response);
            //     }

            // } catch (Exception e) {
            //     ICommand<Void> command = new DecreaseHeartBeat(timeout, hostList);
            //     command.run();
            //     System.out.print(".");
            // }
        // }
    }
}
