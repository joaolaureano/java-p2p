package server.command;

import java.util.HashMap;
import java.util.List;
import java.net.InetAddress;
import server.HostData;

public class CreateCommand implements ICommand<Boolean>{
    List<HostData> hostList;
    HashMap<InetAddress,Integer> timeout;
    HostData newHost;
    public CreateCommand(List<HostData> hostList, HashMap<InetAddress,Integer> timeout,HostData newHost){
        this.hostList = hostList;
        this.newHost = newHost;
        this.timeout = timeout;
    }


    @Override
    public Boolean run() {
        if(hostList.contains(newHost))return false;

        hostList.add(newHost);
        timeout.put(newHost.getAddress(), 15);
        return true;
    }
}
