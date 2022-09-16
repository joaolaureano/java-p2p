package app.command;
import java.net.InetAddress;
import java.util.List;

import app.server.HostData;
import app.socket.Socket;

public class ListCommand implements ICommand<String> {
    List<HostData> connected_hosts;
    public ListCommand(List<HostData> connected_hosts){
        this.connected_hosts = connected_hosts;
    }

    public String run(){
        String content = "";
        for(HostData name : connected_hosts)
            content += name.getHostName() + " - " + name.getAddress().toString() + " - " + name.getHostPort() + "\n";
        return content;
    }
}