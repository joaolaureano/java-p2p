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
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + hostPort;
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
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (hostName == null) {
            if (other.hostName != null)
                return false;
        } else if (!hostName.equals(other.hostName))
            return false;
        if (hostPort != other.hostPort)
            return false;
        return true;
    }

    

}