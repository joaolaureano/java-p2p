package p2p.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeerRegistryTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    /** A clock the test can move forward by hand. */
    static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2024-01-01T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final PeerRegistry registry = new PeerRegistry(TIMEOUT, clock);

    private static PeerInfo peer(String name, String host, int port) {
        try {
            return new PeerInfo(name, InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void registerMakesThePeerActive() {
        PeerInfo alice = peer("alice", "127.0.0.1", 5000);

        assertTrue(registry.register(alice));
        assertTrue(registry.isActive("alice"));
        assertEquals(List.of(alice), registry.activePeers());
    }

    @Test
    void duplicateNameFromAnotherAddressIsRejected() {
        assertTrue(registry.register(peer("alice", "127.0.0.1", 5000)));
        assertFalse(registry.register(peer("alice", "127.0.0.1", 6000)));
        assertFalse(registry.register(peer("alice", "127.0.0.2", 5000)));
        assertEquals(1, registry.activePeers().size());
    }

    @Test
    void reRegisteringFromTheSameAddressRefreshesIt() {
        PeerInfo alice = peer("alice", "127.0.0.1", 5000);
        registry.register(alice);

        clock.advance(Duration.ofSeconds(10));
        assertTrue(registry.register(alice));

        clock.advance(Duration.ofSeconds(10));
        assertTrue(registry.expire().isEmpty());
        assertTrue(registry.isActive("alice"));
    }

    @Test
    void heartbeatPreventsExpiry() {
        PeerInfo alice = peer("alice", "127.0.0.1", 5000);
        registry.register(alice);

        clock.advance(Duration.ofSeconds(10));
        assertEquals(PeerRegistry.HeartbeatResult.REFRESHED, registry.heartbeat(alice));

        clock.advance(Duration.ofSeconds(10));
        assertTrue(registry.expire().isEmpty());
    }

    @Test
    void peersExpireAfterTheTimeout() {
        registry.register(peer("alice", "127.0.0.1", 5000));

        clock.advance(TIMEOUT);
        assertTrue(registry.expire().isEmpty(), "exactly at the timeout the peer is still alive");

        clock.advance(Duration.ofSeconds(1));
        List<PeerInfo> expired = registry.expire();
        assertEquals(1, expired.size());
        assertEquals("alice", expired.get(0).name());
        assertFalse(registry.isActive("alice"));
    }

    @Test
    void heartbeatFromAnotherAddressIsRejected() {
        registry.register(peer("alice", "127.0.0.1", 5000));

        assertEquals(PeerRegistry.HeartbeatResult.REJECTED, registry.heartbeat(peer("alice", "127.0.0.1", 6000)));
    }

    @Test
    void heartbeatAfterExpiryRegistersThePeerAgain() {
        PeerInfo alice = peer("alice", "127.0.0.1", 5000);
        registry.register(alice);

        clock.advance(Duration.ofSeconds(20));
        registry.expire();
        assertFalse(registry.isActive("alice"));

        assertEquals(PeerRegistry.HeartbeatResult.REREGISTERED, registry.heartbeat(alice));
        assertTrue(registry.isActive("alice"));
    }

    @Test
    void activePeersAreSortedByName() {
        registry.register(peer("carol", "127.0.0.1", 3000));
        registry.register(peer("alice", "127.0.0.1", 1000));
        registry.register(peer("bob", "127.0.0.1", 2000));

        assertEquals(List.of("alice", "bob", "carol"),
                registry.activePeers().stream().map(PeerInfo::name).toList());
    }
}
