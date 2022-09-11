package server.command;

import java.net.InetAddress;
import java.util.HashMap;

import server.HostData;

public class HeartBeatCommand implements ICommand<Boolean>{
    HashMap<InetAddress,Integer> timeout;
    HostData hostData;

    public HeartBeatCommand(HashMap<InetAddress,Integer> timeout, HostData hostData ){
        this.timeout = timeout;
        this.hostData = hostData;
    }

    public Boolean run(){
        if(timeout.containsKey(hostData.getAddress())){
        
            System.out.print("\nheartbeat: " + hostData.getHostName());
        this.timeout.put(hostData.getAddress(), 15);
    }
        return true;
    }
}
