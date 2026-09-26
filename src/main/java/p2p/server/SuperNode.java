package p2p.server;

import p2p.dht.HashRange;
import p2p.dht.Md5;
import p2p.dht.ResourceEntry;
import p2p.dht.ResourceTable;
import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.Packet;
import p2p.network.UdpEndpoint;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A super-node of the P2P ring. It owns a contiguous slice of the MD5 hash space, keeps a
 * heartbeat-based registry of its peers and forwards requests around the ring when it does not own
 * the requested hash.
 */
public class SuperNode implements AutoCloseable {

    private static final Duration RECEIVE_TIMEOUT = Duration.ofMillis(500);

    private final SuperNodeConfig config;
    private final UdpEndpoint endpoint;
    private final PeerRegistry registry;
    private final ResourceTable table;
    private final InetAddress selfAddress;
    private final int selfPort;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile InetAddress nextAddress;
    private volatile int nextPort;
    private volatile Thread receiveThread;

    public SuperNode(SuperNodeConfig config) {
        this(config, PeerRegistry.DEFAULT_TIMEOUT, Clock.systemUTC());
    }

    public SuperNode(SuperNodeConfig config, Duration peerTimeout, Clock clock) {
        this.config = config;
        this.table = new ResourceTable(HashRange.forNode(config.position(), config.ringSize()));
        this.registry = new PeerRegistry(peerTimeout, clock);
        this.selfAddress = resolve(config.host());
        this.nextAddress = resolve(config.nextHost());
        this.nextPort = config.nextPort();
        this.endpoint = new UdpEndpoint(config.port());
        this.selfPort = endpoint.localPort();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "supernode-expire-" + selfPort);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Actual bound UDP port of this node. */
    public int port() {
        return selfPort;
    }

    public ResourceTable table() {
        return table;
    }

    public PeerRegistry registry() {
        return registry;
    }

    /** Rewires the next hop of the ring. Used by tests and manual wiring. */
    public void setNext(String host, int port) {
        this.nextAddress = resolve(host);
        this.nextPort = port;
    }

    /** Starts the receive loop and the periodic peer-expiry task. */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(this::receiveLoop, "supernode-receive-" + selfPort);
        thread.setDaemon(true);
        receiveThread = thread;
        thread.start();
        scheduler.scheduleAtFixedRate(this::expirePeers, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        running.set(false);
        scheduler.shutdownNow();
        endpoint.close();
        Thread thread = receiveThread;
        if (thread != null) {
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ---------------------------------------------------------------- loops

    private void receiveLoop() {
        while (!endpoint.isClosed()) {
            try {
                endpoint.receive(RECEIVE_TIMEOUT).ifPresent(this::handlePacket);
            } catch (UncheckedIOException e) {
                if (!endpoint.isClosed()) {
                    log("Receive error: " + e.getMessage());
                }
                return;
            } catch (RuntimeException e) {
                log("Unexpected error in receive loop: " + e);
            }
        }
    }

    private void expirePeers() {
        try {
            for (PeerInfo peer : registry.expire()) {
                log("Peer " + peer.name() + " is INACTIVE");
            }
        } catch (RuntimeException e) {
            log("Error while expiring peers: " + e);
        }
    }

    // ---------------------------------------------------------------- dispatch

    private void handlePacket(Packet packet) {
        Message message;
        try {
            message = Message.parse(packet.data());
        } catch (IllegalArgumentException e) {
            log("Ignoring malformed packet from " + hostPort(packet) + ": " + e.getMessage());
            return;
        }

        try {
            switch (message.type()) {
                case CREATE -> handleCreate(packet, message);
                case HEARTBEAT -> handleHeartbeat(packet, message);
                case REGISTER -> handleRegister(packet, message);
                case REGISTER_RING -> handleRegisterRing(message);
                case LIST -> handleList(packet);
                case LIST_RING -> handleListRing(message);
                case META, META_OK, META_MISSING, CHUNK, CHUNK_DATA ->
                        reply("ERROR unsupported on super-node", packet);
            }
        } catch (RuntimeException e) {
            log("Error handling " + message.type().keyword() + " from " + hostPort(packet) + ": " + e);
        }
    }

    // ---------------------------------------------------------------- handlers

    private void handleCreate(Packet packet, Message message) {
        String name = message.arg(0);
        if (registry.register(new PeerInfo(name, packet.address(), packet.port()))) {
            log("Peer " + name + " joined from " + hostPort(packet));
            reply("OK", packet);
        } else {
            reply("ERROR name already taken: " + name, packet);
        }
    }

    private void handleHeartbeat(Packet packet, Message message) {
        String name = message.arg(0);
        switch (registry.heartbeat(new PeerInfo(name, packet.address(), packet.port()))) {
            case REFRESHED -> { }
            case REREGISTERED -> log("Peer " + name + " re-registered via heartbeat");
            case REJECTED -> log("WARNING: heartbeat for " + name + " from " + hostPort(packet)
                    + " rejected (name held by another address)");
        }
    }

    /** {@code register <hash> <size> <encoded_name>}: the sender becomes the owner of the file. */
    private void handleRegister(Packet packet, Message message) {
        ResourceEntry entry;
        try {
            entry = parseEntry(message.arg(0), message.arg(1), message.arg(2),
                    packet.address().getHostAddress(), packet.port());
        } catch (IllegalArgumentException e) {
            reply("ERROR " + e.getMessage(), packet);
            return;
        }

        if (table.storeIfOwned(entry)) {
            log("Stored " + entry.toWire());
            reply(registeredMessage(entry.hash()), packet);
        } else if (isNextSelf()) {
            reply("ERROR no node owns " + entry.hash(), packet);
        } else {
            sendToNext(Message.of(MessageType.REGISTER_RING, entry.hash(), String.valueOf(entry.size()),
                    message.arg(2), entry.host(), String.valueOf(entry.port()),
                    config.host(), String.valueOf(selfPort)));
        }
    }

    /**
     * {@code register_ring <hash> <size> <encoded_name> <peer_host> <peer_port> <origin_host> <origin_port>}
     */
    private void handleRegisterRing(Message message) {
        String peerHost = message.arg(3);
        int peerPort = parsePort(message.arg(4));
        String originHost = message.arg(5);
        int originPort = parsePort(message.arg(6));

        ResourceEntry entry;
        try {
            entry = parseEntry(message.arg(0), message.arg(1), message.arg(2), peerHost, peerPort);
        } catch (IllegalArgumentException e) {
            sendTo("ERROR " + e.getMessage(), peerHost, peerPort);
            return;
        }

        if (table.storeIfOwned(entry)) {
            log("Stored " + entry.toWire() + " (forwarded by the ring)");
            sendTo(registeredMessage(entry.hash()), peerHost, peerPort);
        } else if (isSelf(originHost, originPort)) {
            // The request went all the way around the ring: nobody owns it, stop here.
            sendTo("ERROR no node owns " + entry.hash(), peerHost, peerPort);
        } else {
            sendToNext(message);
        }
    }

    /** Validates the register fields; the exception message is sent back to the peer after "ERROR ". */
    private static ResourceEntry parseEntry(String hash, String rawSize, String encodedName, String host, int port) {
        if (!Md5.isValidHex(hash)) {
            throw new IllegalArgumentException("invalid hash: " + hash);
        }
        long size;
        try {
            size = Long.parseLong(rawSize);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid size: " + rawSize);
        }
        if (size < 0) {
            throw new IllegalArgumentException("invalid size: " + rawSize);
        }
        String name;
        try {
            name = ResourceEntry.decodeName(encodedName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid name: " + encodedName);
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("invalid name: " + encodedName);
        }
        return new ResourceEntry(hash, host, port, size, name);
    }

    private void handleList(Packet packet) {
        List<String> args = new ArrayList<>();
        args.add(packet.address().getHostAddress());
        args.add(String.valueOf(packet.port()));
        args.add(config.host());
        args.add(String.valueOf(selfPort));
        appendOwnEntries(args);

        if (isNextSelf()) {
            answerList(args);
        } else {
            sendToNext(new Message(MessageType.LIST_RING, args, null));
        }
    }

    /** {@code list_ring <peer_host> <peer_port> <origin_host> <origin_port> [entries...]} */
    private void handleListRing(Message message) {
        List<String> args = new ArrayList<>(message.args());
        if (isSelf(args.get(2), parsePort(args.get(3)))) {
            answerList(args);
            return;
        }
        appendOwnEntries(args);
        sendToNext(new Message(MessageType.LIST_RING, args, null));
    }

    /** Sends the collected list back to the peer that asked for it (not to the previous hop). */
    private void answerList(List<String> args) {
        String peerHost = args.get(0);
        int peerPort = parsePort(args.get(1));
        List<String> entries = args.subList(4, args.size());

        if (entries.isEmpty()) {
            sendTo("NO RESOURCE FOUND", peerHost, peerPort);
            return;
        }
        sendTo("RESOURCES " + entries.size(), peerHost, peerPort);
        for (String entry : entries) {
            sendTo(entry, peerHost, peerPort);
        }
    }

    /** Appends this node's entries to a list_ring payload while it still fits in one packet. */
    private void appendOwnEntries(List<String> args) {
        int size = new Message(MessageType.LIST_RING, args, null).header().getBytes(StandardCharsets.UTF_8).length;
        for (ResourceEntry entry : table.all()) {
            String wire = entry.toWire();
            if (args.contains(wire)) {
                continue;
            }
            int extra = 1 + wire.getBytes(StandardCharsets.UTF_8).length;
            if (size + extra > UdpEndpoint.MAX_PACKET_BYTES) {
                log("WARNING: resource list is full, some entries were left out");
                return;
            }
            args.add(wire);
            size += extra;
        }
    }

    // ---------------------------------------------------------------- helpers

    private String registeredMessage(String hash) {
        return "REGISTERED " + hash + " at node " + config.host() + ":" + selfPort;
    }

    private boolean isSelf(String host, int port) {
        return port == selfPort && (host.equals(config.host()) || resolve(host).equals(selfAddress));
    }

    private boolean isNextSelf() {
        return nextPort == selfPort && nextAddress.equals(selfAddress);
    }

    private void reply(String text, Packet packet) {
        sendText(text, packet.address(), packet.port());
    }

    private void sendTo(String text, String host, int port) {
        sendText(text, resolve(host), port);
    }

    private void sendText(String text, InetAddress address, int port) {
        try {
            endpoint.sendText(text, address, port);
        } catch (IllegalArgumentException e) {
            log("Cannot send to " + address.getHostAddress() + ":" + port + ": " + e.getMessage());
        }
    }

    private void sendToNext(Message message) {
        try {
            endpoint.send(message, nextAddress, nextPort);
        } catch (IllegalArgumentException e) {
            log("Cannot forward to " + nextAddress.getHostAddress() + ":" + nextPort + ": " + e.getMessage());
        }
    }

    private static int parsePort(String raw) {
        try {
            int port = Integer.parseInt(raw);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port out of range: " + raw);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + raw, e);
        }
    }

    private static InetAddress resolve(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + host, e);
        }
    }

    private static String hostPort(Packet packet) {
        return packet.address().getHostAddress() + ":" + packet.port();
    }

    private void log(String message) {
        System.out.println("[node " + selfPort + "] " + message);
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws InterruptedException {
        SuperNodeConfig config;
        try {
            config = SuperNodeConfig.fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        SuperNode node;
        try {
            node = new SuperNode(config);
        } catch (RuntimeException e) {
            System.err.println("Could not start super-node: " + e.getMessage());
            System.exit(1);
            return;
        }

        node.start();
        node.log("Listening on " + config.host() + ":" + node.port()
                + " (position " + config.position() + "/" + config.ringSize() + ")");
        node.log("Owns " + node.table().range());
        node.log("Next node is " + config.nextHost() + ":" + config.nextPort());
        Thread.currentThread().join();
    }
}
