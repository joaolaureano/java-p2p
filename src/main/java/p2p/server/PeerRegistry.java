package p2p.server;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe registry of active peers for a super-node.
 *
 * <p>A peer is identified by its nickname. A nickname can only be held by one address:port pair at a
 * time. Entries are refreshed either by re-registering or by heartbeats, and are removed once they are
 * older than the configured timeout.</p>
 */
public class PeerRegistry {

    /** Default amount of time a peer may stay silent before being expired. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    /** Outcome of a heartbeat for a given peer. */
    public enum HeartbeatResult {
        REFRESHED,
        REREGISTERED,
        REJECTED
    }

    private record Entry(InetAddress address, int port, Instant lastSeen) {
    }

    private final Duration timeout;
    private final Clock clock;
    private final Map<String, Entry> peers = new HashMap<>();

    public PeerRegistry(Duration timeout, Clock clock) {
        this.timeout = timeout;
        this.clock = clock;
    }

    /**
     * Registers a peer. Returns {@code false} if the nickname is already held by a different
     * address:port pair. Re-registering the same address:port refreshes the last seen timestamp.
     */
    public synchronized boolean register(PeerInfo peer) {
        Entry existing = peers.get(peer.name());
        if (existing != null && !sameEndpoint(existing, peer)) {
            return false;
        }
        touch(peer);
        return true;
    }

    /**
     * Processes a heartbeat from a peer.
     * <ul>
     *   <li>same name + address:port: {@link HeartbeatResult#REFRESHED}</li>
     *   <li>unknown name: registered again, {@link HeartbeatResult#REREGISTERED}</li>
     *   <li>name held by another address:port: {@link HeartbeatResult#REJECTED}</li>
     * </ul>
     */
    public synchronized HeartbeatResult heartbeat(PeerInfo peer) {
        Entry existing = peers.get(peer.name());
        if (existing == null) {
            touch(peer);
            return HeartbeatResult.REREGISTERED;
        }
        if (!sameEndpoint(existing, peer)) {
            return HeartbeatResult.REJECTED;
        }
        touch(peer);
        return HeartbeatResult.REFRESHED;
    }

    /** Removes and returns peers whose last-seen timestamp is strictly older than the timeout. */
    public synchronized List<PeerInfo> expire() {
        Instant now = clock.instant();
        List<PeerInfo> expired = new ArrayList<>();
        Iterator<Map.Entry<String, Entry>> iterator = peers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Entry> current = iterator.next();
            Entry entry = current.getValue();
            if (entry.lastSeen().plus(timeout).isBefore(now)) {
                expired.add(new PeerInfo(current.getKey(), entry.address(), entry.port()));
                iterator.remove();
            }
        }
        return expired;
    }

    /** Currently known peers, sorted by nickname. */
    public synchronized List<PeerInfo> activePeers() {
        List<PeerInfo> result = new ArrayList<>();
        for (Map.Entry<String, Entry> entry : peers.entrySet()) {
            result.add(new PeerInfo(entry.getKey(), entry.getValue().address(), entry.getValue().port()));
        }
        result.sort(Comparator.comparing(PeerInfo::name));
        return result;
    }

    /** True when a peer with the given nickname is currently registered. */
    public synchronized boolean isActive(String name) {
        return peers.containsKey(name);
    }

    private void touch(PeerInfo peer) {
        peers.put(peer.name(), new Entry(peer.address(), peer.port(), clock.instant()));
    }

    private static boolean sameEndpoint(Entry entry, PeerInfo peer) {
        return entry.port() == peer.port() && entry.address().equals(peer.address());
    }
}
