package p2p.peer;

import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.UdpEndpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Entry point of a peer: binds a socket, shares the files in its shared folder, announces itself with
 * {@code create}, keeps the registration alive with heartbeats, answers file requests, downloads
 * files on demand and runs the interactive console.
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
            System.err.println(PeerConfig.usage());
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

        SharedFiles files = new SharedFiles();
        try {
            files.addFolder(config.sharedDir());
        } catch (IOException e) {
            System.err.println("Could not read shared folder " + config.sharedDir() + ": " + e.getMessage());
        }

        System.out.println("Nickname:  " + config.nickname());
        System.out.println("Port:      " + endpoint.localPort());
        System.out.println("Shared:    " + config.sharedDir().toAbsolutePath().normalize());
        System.out.println("Downloads: " + config.downloadsDir().toAbsolutePath().normalize());
        System.out.println("Files:     " + files.size());

        FileDownloader downloader = new FileDownloader(endpoint, config.downloadsDir());
        PeerConsole console = new PeerConsole(
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                System.out, endpoint, config, files, downloader, serverAddress);

        HeartbeatSender heartbeat =
                new HeartbeatSender(endpoint, serverAddress, config.serverPort(), config.nickname());

        PeerListener listener = new PeerListener(endpoint, files, downloader, console::rememberListEntry, () -> {
            System.out.println("Nickname rejected by the super-node, shutting down.");
            heartbeat.stop();
            endpoint.close();
            System.exit(1);
        }, System.out);

        Thread listenerThread = new Thread(listener, "peer-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        endpoint.send(Message.of(MessageType.CREATE, config.nickname()), serverAddress, config.serverPort());
        for (SharedFile file : files.all()) {
            endpoint.send(ConsoleCommand.registerMessage(file), serverAddress, config.serverPort());
        }
        heartbeat.start();

        console.run();

        heartbeat.stop();
        endpoint.close();
        System.exit(0);
    }
}
