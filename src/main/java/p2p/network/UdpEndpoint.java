package p2p.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thin wrapper around a {@link DatagramSocket} that sends and receives raw datagrams.
 *
 * <p>A datagram is a UTF-8 header line, optionally followed by {@code 0x0A} and a raw binary body, so
 * the same endpoint carries small control messages and file chunks.</p>
 *
 * <p>Sending is safe from several threads. Only one thread should be receiving at a time, because
 * each {@code receive} call resets the socket timeout.</p>
 */
public class UdpEndpoint implements AutoCloseable {

    /** Largest payload we are willing to send or receive, in bytes. */
    public static final int MAX_PACKET_BYTES = 9216;

    private final DatagramSocket socket;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Binds a UDP socket.
     *
     * @param port the local port, or {@code 0} to let the operating system pick an ephemeral port
     * @throws UncheckedIOException if the port cannot be bound
     */
    public UdpEndpoint(int port) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new UncheckedIOException("Could not bind UDP port " + port + ": " + e.getMessage(), e);
        }
    }

    /** @return the actual local port this endpoint is bound to. */
    public int localPort() {
        return socket.getLocalPort();
    }

    /** @return {@code true} once {@link #close()} has been called. */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Sends raw bytes to the given address and port.
     *
     * <p>Failures are logged to {@code System.err} and never close the socket, so a single bad send
     * does not take the whole node down.</p>
     *
     * @throws IllegalArgumentException if {@code data} is larger than {@link #MAX_PACKET_BYTES}
     */
    public void send(byte[] data, InetAddress address, int port) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(address, "address must not be null");

        if (data.length > MAX_PACKET_BYTES) {
            throw new IllegalArgumentException(
                    "Packet is too large: " + data.length + " bytes (max " + MAX_PACKET_BYTES + ")");
        }

        try {
            socket.send(new DatagramPacket(data, data.length, address, port));
        } catch (IOException e) {
            System.err.println("[endpoint " + socket.getLocalPort() + "] Could not send to "
                    + address.getHostAddress() + ":" + port + ": " + e.getMessage());
        }
    }

    /** Sends a serialized {@link Message}. */
    public void send(Message message, InetAddress address, int port) {
        Objects.requireNonNull(message, "message must not be null");
        send(message.toBytes(), address, port);
    }

    /** Sends plain UTF-8 text (used for simple replies such as {@code OK}). */
    public void sendText(String text, InetAddress address, int port) {
        Objects.requireNonNull(text, "text must not be null");
        send(text.getBytes(StandardCharsets.UTF_8), address, port);
    }

    /**
     * Waits for a packet, returning an empty result when the timeout elapses.
     *
     * @throws UncheckedIOException if the socket has been closed
     */
    public Optional<Packet> receive(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout must not be null");
        ensureOpen();

        int millis = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, timeout.toMillis()));
        try {
            return Optional.of(toPacket(receiveRaw(millis)));
        } catch (SocketTimeoutException e) {
            return Optional.empty();
        } catch (IOException e) {
            if (isClosed() || socket.isClosed()) {
                throw new UncheckedIOException("Socket is closed", e);
            }
            System.err.println("[endpoint " + socket.getLocalPort() + "] Receive failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Waits forever for a packet.
     *
     * @throws UncheckedIOException if the socket has been closed or the receive fails
     */
    public Packet receive() {
        ensureOpen();
        try {
            return toPacket(receiveRaw(0));
        } catch (IOException e) {
            throw new UncheckedIOException("Receive failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            socket.close();
        }
    }

    private DatagramPacket receiveRaw(int soTimeoutMillis) throws IOException {
        socket.setSoTimeout(soTimeoutMillis);
        byte[] buffer = new byte[MAX_PACKET_BYTES];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagram);
        return datagram;
    }

    private static Packet toPacket(DatagramPacket datagram) {
        byte[] data = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(),
                datagram.getOffset() + datagram.getLength());
        return new Packet(datagram.getAddress(), datagram.getPort(), data);
    }

    private void ensureOpen() {
        if (isClosed() || socket.isClosed()) {
            throw new UncheckedIOException("Socket is closed", new SocketException("Socket is closed"));
        }
    }
}
