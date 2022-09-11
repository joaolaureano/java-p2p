package server.command;

import java.net.InetAddress;
import java.util.Map;
import java.util.List;

import server.HostData;

public class DecreaseHeartBeat implements ICommand<Void>{

    public Map<InetAddress,Integer> timeout;
    public List<HostData> hostList;

    public DecreaseHeartBeat(Map<InetAddress, Integer> timeout, List<HostData> hostList){
        this.timeout = timeout;
        this.hostList = hostList;
    }

    public Void run(){

        for( int i = 0 ; i < hostList.size(); i++){
            
            InetAddress currentAddress = hostList.get(i).getAddress();
            int newValue = timeout.get(currentAddress) - 1;
            if(newValue == 0) {
                String message = "User " + hostList.get(i).getHostName() + " is INACTIVE.";
                System.out.println(message);

                timeout.remove(currentAddress);
                hostList.remove(i);
            }
            else {
                timeout.put(currentAddress, newValue);
            }
        }


    return null;
    }
    
}
