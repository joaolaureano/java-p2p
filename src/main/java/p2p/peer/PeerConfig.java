package p2p.peer;

import java.nio.file.Path;

/**
 * Configuration of a peer: its super-node, its nickname, the UDP port it listens on and the folders
 * it shares from and downloads into.
 *
 * <p>A peer port of 0 means "let the operating system pick an ephemeral port".</p>
 */
public record PeerConfig(String serverHost, int serverPort, String nickname, int port,
                         Path sharedDir, Path downloadsDir) {

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
        if (sharedDir == null) {
            throw new IllegalArgumentException("shared_dir must not be null");
        }
        if (downloadsDir == null) {
            throw new IllegalArgumentException("downloads_dir must not be null");
        }
    }

    public static PeerConfig fromArgs(String[] args) {
        if (args == null || args.length < 4 || args.length > 6) {
            throw new IllegalArgumentException("Expected 4 to 6 arguments. " + usage());
        }
        int serverPort;
        int port;
        try {
            serverPort = Integer.parseInt(args[1]);
            port = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ports must be numbers. " + usage(), e);
        }
        Path shared = Path.of(args.length >= 5 ? args[4] : "shared");
        Path downloads = Path.of(args.length >= 6 ? args[5] : "downloads");
        return new PeerConfig(args[0], serverPort, args[2], port, shared, downloads);
    }

    public static String usage() {
        return "Usage: PeerNode <server_host> <server_port> <nickname> <peer_port>"
                + " [shared_dir=shared] [downloads_dir=downloads]";
    }
}
