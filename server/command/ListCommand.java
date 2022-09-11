package server.command;
import java.net.InetAddress;
import java.util.List;

import server.Socket;

public class ListCommand implements ICommand<String> {
    List<String> connected_hosts;
    Socket socket;
    public ListCommand(List<String> connected_hosts, Socket socket){
        this.connected_hosts = connected_hosts;
        this.socket = socket;
    }

    public String run(){
        String content = "";
        for(String name : connected_hosts)
            content += name + "\n";
        return content;
    }
}