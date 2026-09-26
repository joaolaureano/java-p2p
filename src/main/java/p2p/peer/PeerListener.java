package p2p.peer;

import p2p.dht.ResourceEntry;
import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.Packet;
import p2p.network.UdpEndpoint;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Listens for packets addressed to this peer.
 *
 * <p>It answers {@code meta}/{@code chunk} requests for the files it shares, forwards download
 * replies to the {@link FileDownloader}, prints everything else, and reports a list entry to the
 * console. A super-node rejecting our nickname is fatal for this peer.</p>
 */
public final class PeerListener implements Runnable {

    private static final Duration RECEIVE_TIMEOUT = Duration.ofMillis(500);

    private final UdpEndpoint endpoint;
    private final SharedFiles files;
    private final FileDownloader downloader;
    private final Consumer<ResourceEntry> onListEntry;
    private final Runnable onFatal;
    private final PrintStream out;

    public PeerListener(UdpEndpoint endpoint, SharedFiles files, FileDownloader downloader,
                        Consumer<ResourceEntry> onListEntry, Runnable onFatal, PrintStream out) {
        this.endpoint = endpoint;
        this.files = files;
        this.downloader = downloader;
        this.onListEntry = onListEntry;
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
        Message message = null;
        try {
            message = Message.parse(packet.data());
        } catch (IllegalArgumentException ignored) {
            // Plain text (OK, REGISTERED ..., list entries, ERROR ...): handled below.
        }

        if (message != null) {
            handleMessage(message, packet);
        } else {
            handleText(packet);
        }
    }

    private void handleMessage(Message message, Packet packet) {
        try {
            switch (message.type()) {
                case META -> handleMeta(message, packet);
                case CHUNK -> handleChunk(message, packet);
                case META_OK, META_MISSING, CHUNK_DATA -> downloader.deliver(message);
                default -> out.println("<- " + format(packet) + ": " + message.header());
            }
        } catch (RuntimeException e) {
            out.println("Error handling " + message.header() + ": " + e.getMessage());
        }
    }

    private void handleMeta(Message message, Packet packet) {
        String hash = message.arg(0);
        Optional<SharedFile> file = files.get(hash);
        if (file.isEmpty()) {
            reply(Message.of(MessageType.META_MISSING, hash), packet);
            return;
        }
        SharedFile shared = file.get();
        int chunks = SharedFiles.chunkCount(shared.size());
        reply(Message.of(MessageType.META_OK, shared.hash(), Long.toString(shared.size()),
                Integer.toString(chunks), ResourceEntry.encodeName(shared.name())), packet);
    }

    private void handleChunk(Message message, Packet packet) {
        String hash = message.arg(0);
        int index;
        try {
            index = Integer.parseInt(message.arg(1));
        } catch (NumberFormatException e) {
            out.println("<- " + format(packet) + ": invalid chunk index '" + message.arg(1) + "'");
            return;
        }
        if (files.get(hash).isEmpty()) {
            out.println("<- " + format(packet) + ": chunk for unknown hash " + hash);
            return;
        }
        try {
            byte[] chunk = files.readChunk(hash, index);
            reply(Message.withBody(MessageType.CHUNK_DATA, chunk, hash, Integer.toString(index)), packet);
        } catch (IllegalArgumentException e) {
            out.println("<- " + format(packet) + ": " + e.getMessage());
        } catch (IOException e) {
            out.println("Could not read chunk " + index + " of " + hash + ": " + e.getMessage());
        }
    }

    private void handleText(Packet packet) {
        String text = new String(packet.data(), StandardCharsets.UTF_8);
        try {
            ResourceEntry entry = ResourceEntry.fromWire(text.trim());
            out.println("<- " + format(packet) + ": " + entry.hash() + "  " + entry.size() + "  "
                    + entry.name() + "  @" + entry.host() + ":" + entry.port());
            if (onListEntry != null) {
                onListEntry.accept(entry);
            }
            return;
        } catch (IllegalArgumentException ignored) {
            // Not a list entry: fall through and print the raw text.
        }

        out.println("<- " + format(packet) + ": " + text);
        if (text.startsWith("ERROR name already taken")) {
            onFatal.run();
        }
    }

    private void reply(Message message, Packet to) {
        try {
            endpoint.send(message, to.address(), to.port());
        } catch (RuntimeException e) {
            out.println("Could not reply to " + format(to) + ": " + e.getMessage());
        }
    }

    private static String format(Packet packet) {
        return packet.address().getHostAddress() + ":" + packet.port();
    }
}
