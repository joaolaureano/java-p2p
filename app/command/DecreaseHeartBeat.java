package app.command;

import java.net.InetAddress;
import java.util.Map;

import app.server.HostData;

import java.util.List;
import java.util.ListIterator;

public class DecreaseHeartBeat implements ICommand<Void> {

    public Map<String, Integer> timeout;
    public List<HostData> hostList;

    public DecreaseHeartBeat(Map<String, Integer> timeout, List<HostData> hostList) {
        this.timeout = timeout;
        this.hostList = hostList;
    }

    public Void run() {
        ListIterator<HostData> iterator = hostList.listIterator();
        while (iterator.hasNext()) {
            HostData nextHostData = iterator.next();
            int newValue = timeout.get(nextHostData.getHostName()) - 1;
            if (newValue == 0) {
                String message = "User " + nextHostData.getHostName() + " is INACTIVE.";
                System.out.println(message);

                timeout.remove(nextHostData.getHostName());
                iterator.remove();
            } else {
                timeout.put(nextHostData.getHostName(), newValue);
            }
        }
        return null;
    }

}
