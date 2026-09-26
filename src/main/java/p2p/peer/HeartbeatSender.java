package p2p.peer;

import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.UdpEndpoint;

import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends {@code heartbeat <nickname>} to the super-node every {@link #INTERVAL}, using the peer's own
 * UDP endpoint so the super-node sees the same address:port that registered the nickname.
 */
public final class HeartbeatSender {

    public static final Duration INTERVAL = Duration.ofSeconds(5);

    private final UdpEndpoint endpoint;
    private final InetAddress server;
    private final int serverPort;
    private final String nickname;

    private ScheduledExecutorService scheduler;

    public HeartbeatSender(UdpEndpoint endpoint, InetAddress server, int serverPort, String nickname) {
        this.endpoint = endpoint;
        this.server = server;
        this.serverPort = serverPort;
        this.nickname = nickname;
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "peer-heartbeat-" + nickname);
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void sendHeartbeat() {
        if (endpoint.isClosed()) {
            return;
        }
        try {
            endpoint.send(Message.of(MessageType.HEARTBEAT, nickname), server, serverPort);
        } catch (RuntimeException e) {
            System.err.println("Heartbeat failed for " + nickname + ": " + e.getMessage());
        }
    }
}
