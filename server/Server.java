package server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import server.Socket.SocketPayload;
import server.command.CreateCommand;
import server.command.DecreaseHeartBeat;
import server.command.HeartBeatCommand;
import server.command.ICommand;

public class Server {
    static Socket socket;
    static int port = 9000;

    static HashMap<InetAddress,Integer> timeout = new HashMap<InetAddress,Integer>();
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
                
                String response;
                if(commandResult){
                    response = "OK"	;
                } else {
                    response = "NOT OK";
				}
                System.out.println("Here");
                socket.sendPacket(response, address, port);
            }

            if(vars[0].equals("heartbeat") && vars.length > 1){
                HostData hostData = new HostData(address, hostName, port);
                ICommand<Boolean> command = new HeartBeatCommand(timeout, hostData );
                command.run();
                
            }
        } catch (Exception e) {
            
            ICommand<Void> command = new DecreaseHeartBeat(timeout, hostList);
            command.run();
            System.out.print(".");
        }
    }
    }
}
