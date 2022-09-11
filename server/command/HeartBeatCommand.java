package server.command;

import java.util.HashMap;

import server.HostData;

public class HeartBeatCommand implements ICommand<Boolean>{
    HashMap<String,Integer> timeout;
    HostData hostData;

    public HeartBeatCommand(HashMap<String,Integer> timeout, HostData hostData ){
        this.timeout = timeout;
        this.hostData = hostData;
    }

    public Boolean run(){
        if(timeout.containsKey(hostData.getHostName())){
        
            System.out.print("\nheartbeat: " + hostData.getHostName());
        this.timeout.put(hostData.getHostName(), 15);
    }
        return true;
    }
}
