package app.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.bucket.DHTBucket;
import app.bucket.DHTHashMd5;
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
    static int next_sp;

    static HashMap<String, Integer> timeout = new HashMap<String, Integer>();
    static List<HostData> hostList = new ArrayList<>();
    static IDHTBucket<String> bucket;

    public static void main(String[] args) throws IOException {
        // port = Integer.parseInt(args[0]);
        port = 9001;

        socket = new Socket(port);
        // next_sp = Integer.parseInt(args[1]);
        next_sp = 9000;

        // int ring_position = Integer.parseInt(args[2]);
        int ring_position = 2;

        // int ring_size = Integer.parseInt(args[3]);
        int ring_size = 2;

        System.out.println("SETUP SERVER AT PORT -> " + port);
        System.out.println("NEXT SERVER PORT -> " + next_sp);

        bucket = new DHTBucket<String>(ring_size, ring_position);

        while (true) {
            try {
                SocketPayload socketPayload = socket.receivePacket();

                String vars[] = socketPayload.getContent().split("\\s");
                InetAddress address = socketPayload.getAddress();
                int port = socketPayload.getPort();
                String hostName = vars[1];
                
                System.out.println(String.join(" ", vars));

                if (vars[0].equals("create") && vars.length > 1) {
                    HostData hostData = new HostData(address, hostName, port);
                    ICommand<Boolean> command = new CreateCommand(hostList, timeout, hostData);
                    boolean commandResult = command.run();

                    String response = commandResult ? "OK" : "NOT OK";

                    socket.sendPacket(response, address, port);
                }
                if (vars[0].equals("heartbeat") && vars.length > 1) {
                    HostData hostData = new HostData(address, hostName, port);
                    ICommand<Boolean> command = new HeartBeatCommand(timeout, hostData);
                    command.run();

                }
                if (vars[0].equals("list") && vars.length > 1) {

                    ICommand<String> command = new ListCommand(hostList);
                    String[] response = command.run().split("\r?\n|\r");

                    for (String responseEntry : response) {
                        socket.sendPacket(responseEntry, address, port);
                    }
                }
                if (vars[0].equals("register") && vars.length > 1) {

                    String hash = vars[2];

                    InetAddress host = InetAddress.getByName("localhost");

                    ICommand<Boolean> command = new RegisterCommand(bucket, hash, "DUMB DATA");

                    boolean response = command.run();
                    if(response){
                        System.out.println("DATA STORED.");
                        

                    }
                    else{
                        // String response2 = String.join(" ", vars);
                        
                        socket.sendPacket(String.join(" ", vars), host, next_sp);
                    }

                }
                // 9223372036854775805
            } catch (Exception e) {
                ICommand<Void> command = new DecreaseHeartBeat(timeout, hostList);
                command.run();
                System.out.print(".");
            }
        }
    }
}
