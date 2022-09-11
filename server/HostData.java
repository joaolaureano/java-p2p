package server;

import java.net.InetAddress;

public class HostData {
    InetAddress address;
    String hostName;
    int hostPort;
    
    public HostData(InetAddress address, String hostName, int hostPort){
        this.address = address;
        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    public InetAddress getAddress() {
        return address;
    }

    public String getHostName() {
        return hostName;
    }

    public int getHostPort() {
        return hostPort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostData other = (HostData) obj;
        if (hostName == null) {
            if (other.hostName != null)
                return false;
        } else if (!hostName.equals(other.hostName))
            return false;
        return true;
    }

    

}