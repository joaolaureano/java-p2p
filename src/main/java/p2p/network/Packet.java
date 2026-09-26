package p2p.network;

import java.net.InetAddress;
import java.util.Objects;

/**
 * A received UDP datagram: who sent it plus the exact bytes that arrived.
 *
 * <p>{@code data} is the raw payload (a UTF-8 header line, optionally followed by {@code 0x0A} and a
 * binary body), kept exactly as received.</p>
 *
 * @param address address of the sender
 * @param port    port of the sender
 * @param data    exact received bytes
 */
public record Packet(InetAddress address, int port, byte[] data) {

    public Packet {
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(data, "data must not be null");
    }
}
