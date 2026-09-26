package p2p.network;

import java.net.InetAddress;
import java.util.Objects;

/**
 * A single UDP datagram received by a {@link UdpEndpoint}.
 *
 * @param address the address the packet came from
 * @param port    the port the packet came from
 * @param content the decoded text content of the packet
 */
public record Packet(InetAddress address, int port, String content) {

    public Packet {
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
