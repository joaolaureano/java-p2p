package p2p.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import p2p.network.Packet;
import p2p.network.UdpEndpoint;

/** Runs a real 3-node ring on localhost (ephemeral ports) and talks to it over UDP. */
class SuperNodeRingTest {

    private static final String LOCALHOST = "127.0.0.1";
    private static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(2);
    private static final String HASH_NODE_1 = "00000000000000000000000000000001";
    private static final String HASH_NODE_2 = "60000000000000000000000000000000";
    private static final String HASH_NODE_3 = "ffffffffffffffffffffffffffffffff";

    private final List<SuperNode> nodes = new ArrayList<>();
    private final List<UdpEndpoint> clients = new ArrayList<>();

    @AfterEach
    void tearDown() {
        clients.forEach(UdpEndpoint::close);
        nodes.forEach(SuperNode::close);
    }

    private SuperNode[] startRing() {
        SuperNode[] ring = new SuperNode[3];
        for (int position = 1; position <= 3; position++) {
            SuperNodeConfig config = new SuperNodeConfig(0, LOCALHOST, position, 3, LOCALHOST, 1);
            SuperNode node = new SuperNode(config, Duration.ofSeconds(60), Clock.systemUTC());
            nodes.add(node);
            ring[position - 1] = node;
        }
        for (int i = 0; i < 3; i++) {
            ring[i].setNext(LOCALHOST, ring[(i + 1) % 3].port());
            ring[i].start();
        }
        return ring;
    }

    private UdpEndpoint newClient() {
        UdpEndpoint client = new UdpEndpoint(0);
        clients.add(client);
        return client;
    }

    private static InetAddress localhost() {
        try {
            return InetAddress.getByName(LOCALHOST);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String receive(UdpEndpoint client) {
        Optional<Packet> packet = client.receive(RECEIVE_TIMEOUT);
        assertTrue(packet.isPresent(), "expected a UDP packet within " + RECEIVE_TIMEOUT);
        return packet.get().content();
    }

    @Test
    void registerStoresEachHashOnItsOwnerAndListReturnsEverything() {
        SuperNode[] ring = startRing();
        UdpEndpoint client = newClient();

        for (String hash : List.of(HASH_NODE_1, HASH_NODE_2, HASH_NODE_3)) {
            client.send("register " + hash, localhost(), ring[0].port());
            String reply = receive(client);
            assertTrue(reply.startsWith("REGISTERED " + hash), "reply was: " + reply);
        }

        assertEquals(1, ring[0].table().size());
        assertEquals(1, ring[1].table().size());
        assertEquals(1, ring[2].table().size());
        assertTrue(ring[0].table().get(HASH_NODE_1).isPresent());
        assertTrue(ring[1].table().get(HASH_NODE_2).isPresent());
        assertTrue(ring[2].table().get(HASH_NODE_3).isPresent());

        // Ask a different node than the one used to register: the answer must reach the client.
        client.send("list", localhost(), ring[1].port());
        assertEquals("RESOURCES 3", receive(client));

        String expectedOwner = "@" + LOCALHOST + ":" + client.localPort();
        List<String> entries = List.of(receive(client), receive(client), receive(client));
        for (String entry : entries) {
            assertTrue(entry.endsWith(expectedOwner), "entry was: " + entry);
        }
        assertEquals(3, entries.stream().distinct().count());
    }

    @Test
    void listOnAnEmptyRingSaysNoResourceFound() {
        SuperNode[] ring = startRing();
        UdpEndpoint client = newClient();

        client.send("list", localhost(), ring[0].port());
        assertEquals("NO RESOURCE FOUND", receive(client));
    }

    @Test
    void malformedPacketsDoNotKillTheNode() {
        SuperNode[] ring = startRing();
        UdpEndpoint client = newClient();

        client.send("", localhost(), ring[0].port());
        client.send("garbage", localhost(), ring[0].port());
        client.send("register", localhost(), ring[0].port());
        client.send("register_ring x y notaport z 1", localhost(), ring[0].port());

        client.send("create alice", localhost(), ring[0].port());
        assertEquals("OK", receive(client));
    }

    @Test
    void invalidHashIsRejected() {
        SuperNode[] ring = startRing();
        UdpEndpoint client = newClient();

        client.send("register not-a-hash", localhost(), ring[0].port());
        assertEquals("ERROR invalid hash: not-a-hash", receive(client));
    }

    @Test
    void duplicateNicknameFromAnotherPeerIsRejected() {
        SuperNode[] ring = startRing();
        UdpEndpoint first = newClient();
        UdpEndpoint second = newClient();

        first.send("create bob", localhost(), ring[0].port());
        assertEquals("OK", receive(first));

        second.send("create bob", localhost(), ring[0].port());
        assertEquals("ERROR name already taken: bob", receive(second));
    }

    @Test
    void registerStopsAtTheOriginWhenNoNodeOwnsTheHash() {
        SuperNode[] ring = startRing();
        // Take node 2 out of the ring: nobody owns its slice any more.
        ring[0].setNext(LOCALHOST, ring[2].port());
        ring[2].setNext(LOCALHOST, ring[0].port());
        UdpEndpoint client = newClient();

        client.send("register " + HASH_NODE_2, localhost(), ring[0].port());

        String reply = receive(client);
        assertTrue(reply.startsWith("ERROR no node owns " + HASH_NODE_2), "reply was: " + reply);
        assertEquals(0, ring[0].table().size());
        assertEquals(0, ring[2].table().size());
    }
}
