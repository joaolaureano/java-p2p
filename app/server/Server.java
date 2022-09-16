package app.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.command.CreateCommand;
import app.command.DecreaseHeartBeat;
import app.command.HeartBeatCommand;
import app.command.ICommand;
import app.command.ListCommand;
import app.socket.Socket;
import app.socket.Socket.SocketPayload;

public class Server {
    static Socket socket;
    static int port = 9000;

    static HashMap<String,Integer> timeout = new HashMap<String,Integer>();
    static List<HostData> hostList = new ArrayList<>();


    public static void main(String[] args) throws IOException{
        socket = new Socket(port);
        while(true){
            try {
            SocketPayload socketPayload = socket.receivePacket();

            String vars[] = socketPayload.getContent().split("\\s");
            InetAddress address = socketPayload.getAddress();
            int port = socketPayload.getPort();
            String hostName = vars[1];
            
            if(vars[0].equals("create") && vars.length > 1){
                HostData hostData = new HostData(address,hostName,port);
                ICommand<Boolean> command = new CreateCommand(hostList, timeout, hostData);
                boolean commandResult = command.run();
                
                String response = commandResult ? "OK" : "NOT OK";
                
                socket.sendPacket(response, address, port);
            }

            if(vars[0].equals("heartbeat") && vars.length > 1){
                HostData hostData = new HostData(address, hostName, port);
                ICommand<Boolean> command = new HeartBeatCommand(timeout, hostData );
                command.run();
                
            }
            if(vars[0].equals("list") && vars.length > 1){

                ICommand<String> command = new ListCommand(hostList);
                String[] response = command.run().split("\r?\n|\r");

                for(String responseEntry: response){
                    socket.sendPacket(responseEntry, address, port);
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
