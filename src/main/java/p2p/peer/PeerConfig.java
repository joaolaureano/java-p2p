package p2p.peer;

/**
 * Configuration of a peer: its super-node, its nickname and the UDP port it listens on.
 * A peer port of 0 means "let the operating system pick an ephemeral port".
 */
public record PeerConfig(String serverHost, int serverPort, String nickname, int port) {

    public PeerConfig {
        if (serverHost == null || serverHost.isBlank()) {
            throw new IllegalArgumentException("server_host must not be blank");
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("server_port must be between 1 and 65535, got " + serverPort);
        }
        if (nickname == null || nickname.isBlank() || nickname.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("nickname must be a single non-blank word");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("peer_port must be between 0 and 65535, got " + port);
        }
    }

    public static PeerConfig fromArgs(String[] args) {
        if (args == null || args.length != 4) {
            throw new IllegalArgumentException("Expected 4 arguments. " + usage());
        }
        int serverPort;
        int port;
        try {
            serverPort = Integer.parseInt(args[1]);
            port = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ports must be numbers. " + usage(), e);
        }
        return new PeerConfig(args[0], serverPort, args[2], port);
    }

    public static String usage() {
        return "Usage: PeerNode <server_host> <server_port> <nickname> <peer_port>";
    }
}
