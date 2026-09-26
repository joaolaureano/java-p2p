package p2p.peer;

import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.Packet;
import p2p.network.UdpEndpoint;

import java.io.PrintStream;
import java.time.Duration;
import java.util.Optional;

/**
 * Listens for packets addressed to this peer.
 *
 * <p>A {@code resource <hash>} request is answered with "I HAVE THIS CONTENT!" plus the content, or
 * with "I DO NOT HAVE THIS CONTENT...". Everything else is printed. A super-node rejecting our
 * nickname is fatal for this peer.</p>
 */
public final class PeerListener implements Runnable {

    private static final Duration RECEIVE_TIMEOUT = Duration.ofMillis(500);

    private final UdpEndpoint endpoint;
    private final SharedResource resource;
    private final Runnable onFatal;
    private final PrintStream out;

    public PeerListener(UdpEndpoint endpoint, SharedResource resource, Runnable onFatal, PrintStream out) {
        this.endpoint = endpoint;
        this.resource = resource;
        this.onFatal = onFatal;
        this.out = out;
    }

    @Override
    public void run() {
        while (!endpoint.isClosed()) {
            try {
                Optional<Packet> received = endpoint.receive(RECEIVE_TIMEOUT);
                received.ifPresent(this::handle);
            } catch (RuntimeException e) {
                if (endpoint.isClosed()) {
                    return;
                }
                out.println("Error receiving packet: " + e.getMessage());
            }
        }
    }

    private void handle(Packet packet) {
        String content = packet.content();

        Message message = null;
        try {
            message = Message.parse(content);
        } catch (IllegalArgumentException ignored) {
            // Plain text answer (OK, REGISTERED ..., list entries ...): just print it below.
        }

        if (message != null && message.type() == MessageType.RESOURCE) {
            handleResourceRequest(message.arg(0), packet);
            return;
        }

        out.println("<- " + format(packet) + ": " + content);

        if (content.startsWith("ERROR name already taken")) {
            onFatal.run();
        }
    }

    private void handleResourceRequest(String hash, Packet packet) {
        if (resource.matches(hash)) {
            reply("I HAVE THIS CONTENT!", packet);
            reply(resource.content(), packet);
        } else {
            reply("I DO NOT HAVE THIS CONTENT...", packet);
        }
    }

    private void reply(String text, Packet to) {
        try {
            endpoint.send(text, to.address(), to.port());
        } catch (RuntimeException e) {
            out.println("Could not reply to " + format(to) + ": " + e.getMessage());
        }
    }

    private static String format(Packet packet) {
        return packet.address().getHostAddress() + ":" + packet.port();
    }
}
