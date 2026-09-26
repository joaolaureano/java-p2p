package p2p.server;

import p2p.dht.HashRange;

/**
 * Immutable configuration of a super-node.
 *
 * @param port     the UDP port to bind (0 means an ephemeral port)
 * @param host     the host advertised to other nodes and peers
 * @param position 1-based position of this node in the ring
 * @param ringSize total number of nodes in the ring
 * @param nextHost host of the next node in the ring
 * @param nextPort UDP port of the next node in the ring
 */
public record SuperNodeConfig(int port, String host, int position, int ringSize,
                              String nextHost, int nextPort) {

    private static final String DEFAULT_HOST = "127.0.0.1";

    /**
     * Parses super-node arguments: {@code <port> <next_port> <ring_position> <ring_size> [next_host] [host]}.
     *
     * @throws IllegalArgumentException when the argument count or any value is invalid
     */
    public static SuperNodeConfig fromArgs(String[] args) {
        if (args == null || args.length < 4 || args.length > 6) {
            throw new IllegalArgumentException("Expected 4 to 6 arguments. " + usage());
        }

        int port = parsePort(args[0], "port", 0);
        int nextPort = parsePort(args[1], "next_port", 1);
        int position = parseInt(args[2], "ring_position");
        int ringSize = parseInt(args[3], "ring_size");
        String nextHost = args.length >= 5 ? args[4] : DEFAULT_HOST;
        String host = args.length >= 6 ? args[5] : DEFAULT_HOST;

        if (nextHost.isBlank() || host.isBlank()) {
            throw new IllegalArgumentException("Hosts must not be blank. " + usage());
        }

        try {
            HashRange.forNode(position, ringSize);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + ". " + usage());
        }

        return new SuperNodeConfig(port, host, position, ringSize, nextHost, nextPort);
    }

    /** Human readable usage message. */
    public static String usage() {
        return "Usage: SuperNode <port> <next_port> <ring_position> <ring_size> [next_host] [host]";
    }

    private static int parseInt(String raw, String field) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + ": " + raw + ". " + usage());
        }
    }

    private static int parsePort(String raw, String field, int min) {
        int value = parseInt(raw, field);
        if (value < min || value > 65535) {
            throw new IllegalArgumentException(
                    field + " must be between " + min + " and 65535. " + usage());
        }
        return value;
    }
}
