package p2p.server;

import java.net.InetAddress;

/**
 * Immutable description of a peer known to a super-node.
 *
 * @param name    the peer nickname
 * @param address the peer IP address
 * @param port    the peer UDP port
 */
public record PeerInfo(String name, InetAddress address, int port) {
}
