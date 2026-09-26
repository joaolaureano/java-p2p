package p2p.peer;

import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.UdpEndpoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Entry point of a peer: binds a socket, announces itself with {@code create}, keeps the registration
 * alive with heartbeats, answers resource requests and runs the interactive console.
 */
public final class PeerNode {

    private PeerNode() {
    }

    public static void main(String[] args) {
        PeerConfig config;
        try {
            config = PeerConfig.fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        InetAddress serverAddress;
        try {
            serverAddress = InetAddress.getByName(config.serverHost());
        } catch (UnknownHostException e) {
            System.err.println("Unknown server host: " + config.serverHost());
            System.exit(1);
            return;
        }

        UdpEndpoint endpoint;
        try {
            endpoint = new UdpEndpoint(config.port());
        } catch (RuntimeException e) {
            System.err.println("Could not start peer: " + e.getMessage());
            System.exit(1);
            return;
        }

        SharedResource resource = new SharedResource(config.nickname() + "_" + UUID.randomUUID());
        System.out.println("Nickname: " + config.nickname());
        System.out.println("Port:     " + endpoint.localPort());
        System.out.println("Content:  " + resource.content());
        System.out.println("Hash:     " + resource.hash());

        HeartbeatSender heartbeat =
                new HeartbeatSender(endpoint, serverAddress, config.serverPort(), config.nickname());

        PeerListener listener = new PeerListener(endpoint, resource, () -> {
            System.out.println("Nickname rejected by the super-node, shutting down.");
            heartbeat.stop();
            endpoint.close();
            System.exit(1);
        }, System.out);

        Thread listenerThread = new Thread(listener, "peer-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        endpoint.send(Message.of(MessageType.CREATE, config.nickname()).format(),
                serverAddress, config.serverPort());
        heartbeat.start();

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new PeerConsole(input, System.out, endpoint, config, resource).run();

        heartbeat.stop();
        endpoint.close();
        System.exit(0);
    }
}
