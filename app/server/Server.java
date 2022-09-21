package app.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.bucket.DHTBucket;
import app.bucket.DHTHashMd5;
import app.bucket.IDHTBucket;
import app.command.CreateCommand;
import app.command.DecreaseHeartBeat;
import app.command.HeartBeatCommand;
import app.command.ICommand;
import app.command.ListCommand;
import app.command.ListResourseCommand;
import app.command.RegisterCommand;
import app.socket.Socket;
import app.socket.Socket.SocketPayload;
import app.bucket.BucketResource;

public class Server {
    static Socket socket;
    static int port;
    static int next_sp;

    static HashMap<String, Integer> timeout = new HashMap<String, Integer>();
    static List<HostData> hostList = new ArrayList<>();
    static IDHTBucket<BucketResource> bucket;

    public static void main(String[] args) throws IOException {
        // port = 9001;
        // next_sp = 9000;
        // int ring_size = 2;
        // int ring_position =2;
        
        
        
        port = Integer.parseInt(args[0]);
        int ring_position = Integer.parseInt(args[2]);
        int ring_size = Integer.parseInt(args[3]);
        next_sp = Integer.parseInt(args[1]);
        
        
        socket = new Socket(port);
        System.out.println("SETUP SERVER AT PORT -> " + port);
        System.out.println("NEXT SERVER PORT -> " + next_sp);

        bucket = new DHTBucket<BucketResource>(ring_size, ring_position);

        while (true) {
            try {
                SocketPayload socketPayload = socket.receivePacket();

                String vars[] = socketPayload.getContent().split("\\s");
                InetAddress address = socketPayload.getAddress();
                int port = socketPayload.getPort();
                String hostName = vars[1];

                // System.out.println(String.join(" ", vars) + "\n");

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

                    // tu precisa
                    // pegar a lista atual
                    ICommand<String> command = new ListResourseCommand(Server.bucket);

                    String response = command.run();
                    // montar a request com <list_ring>
                    String request = "list_ring " + response + " " + port + " " + Server.port;

                    // mandar pro próximo
                    socket.sendPacket(request, address, next_sp);

                }
                if (vars[0].equals("register") && vars.length > 1) {

                    String hash = vars[1];

                    InetAddress host = InetAddress.getByName("localhost");

                    BucketResource bResource = new BucketResource(hash, port);

                    ICommand<Boolean> command = new RegisterCommand(bucket, bResource);

                    boolean response = command.run();
                    if (response) {

                        System.out.println("DATA STORED.");

                    } else {

                        System.out.println("DATA STORED BY RING.");

                        String nextPeerResource = "register_ring " + hash + " " + port + " " + Server.port;
                        
                        socket.sendPacket(nextPeerResource, host, next_sp);

                    }

                }
                if (vars[0].equals("register_ring") && vars.length > 1) {
                    System.out.println("REGISTER_RING PROCEDURE.");

                    int clientPort = Integer.parseInt(vars[2]);

                    String hash = vars[1];

                    BucketResource bResource = new BucketResource(hash, clientPort);

                    ICommand<Boolean> command = new RegisterCommand(bucket, bResource);

                    boolean response = command.run();

                    InetAddress host = InetAddress.getByName("localhost");
                    if (response) {

                        System.out.println("DATA STORED BY RING.");

                    } else {
                        System.out.println("REGISTER RING SENDING PACKET.");
                        socket.sendPacket(String.join(" ", vars), host, next_sp);
                    }

                }
                if (vars[0].equals("list_ring") && vars.length > 1) {


                    String oldList = vars[1];
                    String peerPort = vars[vars.length - 2];
                    String superPeerPort = vars[vars.length - 1];

                    if (Integer.parseInt(superPeerPort) == Server.port) { // se voltou pro inicio
                        if(!oldList.isEmpty()){
                            System.out.println("RECOVERED LIST. SENDING TO REQUESTER");
                            socket.sendPacket("CONTENT_HASH-PEER_PORT", address, Integer.parseInt(peerPort));
                            for (String responseEntry : oldList.split(";")) {
                                socket.sendPacket(responseEntry, address, Integer.parseInt(peerPort));
                            }
                        }   
                        else{
                            String message = "NO RESOURCE FOUND";

                            socket.sendPacket(message, address, Integer.parseInt(peerPort));
                        }
                    } else {

                        ICommand<String> command = new ListResourseCommand(Server.bucket);

                        String response = command.run();

                        String newList = response + oldList;

                        System.out.println();

                        response = "list_ring " + newList + " " + peerPort + " " + superPeerPort;
                        socket.sendPacket(response, address, next_sp);

                    }
                }
            } catch (Exception e) {
                ICommand<Void> command = new DecreaseHeartBeat(timeout, hostList);
                command.run();
                System.out.print(".");
            }
        }
    }
}
