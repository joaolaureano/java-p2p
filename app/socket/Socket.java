package app.socket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Socket {
    int TIMEOUT = 500;
    int BYTE = 1024;
    DatagramSocket datagramSocket;

    public Socket(int port) {
        try {
            this.datagramSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendPacket(String content, InetAddress addr, int port) {
        try {
            byte[] contentBytes = content.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(contentBytes, content.length(), addr, port);
            datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            // this.close();
        }
    }

    public SocketPayload receivePacket(){
        try {
            byte[] buffer = new byte[BYTE];
            
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramSocket.setSoTimeout(TIMEOUT);
            datagramSocket.receive(datagramPacket);

            String content = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

            return new SocketPayload(datagramPacket.getAddress(), datagramPacket.getPort(), content);

        } catch (IOException e) {
            
        }

        return null;
    }

    public void close(){
        this.datagramSocket.close();
    }

    public class SocketPayload {
        private InetAddress address;
        private int port;
        private String content;

        protected SocketPayload(InetAddress address, int port, String content){
            this.address = address;
            this.port = port;
            this.content = content;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getContent() {
            return content;
        }
    }
}