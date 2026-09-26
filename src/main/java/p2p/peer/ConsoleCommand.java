package p2p.peer;

import p2p.dht.Md5;
import p2p.network.Message;
import p2p.network.MessageType;

import java.util.Locale;

/**
 * A single command typed on the peer console, translated into an optional network message plus the
 * host/port it must be sent to. Parsing is pure: it never touches the network.
 */
public record ConsoleCommand(Kind kind, Message message, String targetHost, int targetPort) {

    public enum Kind { SEND, HELP, INFO, QUIT, EMPTY }

    /**
     * Parses one console line.
     *
     * @throws IllegalArgumentException for unknown commands or wrong arguments (never any other exception)
     */
    public static ConsoleCommand parse(String line, PeerConfig config, SharedResource resource) {
        if (line == null || line.isBlank()) {
            return local(Kind.EMPTY);
        }

        String[] tokens = line.trim().split("\\s+");
        String command = tokens[0].toLowerCase(Locale.ROOT);

        return switch (command) {
            case "register" -> sendToServerOrTarget(tokens, Message.of(MessageType.REGISTER, resource.hash()),
                    config, "register [server_host server_port]");
            case "list" -> sendToServerOrTarget(tokens, Message.of(MessageType.LIST),
                    config, "list [server_host server_port]");
            case "resource" -> parseResource(tokens);
            case "info" -> noArguments(tokens, Kind.INFO);
            case "help" -> noArguments(tokens, Kind.HELP);
            case "quit", "exit" -> noArguments(tokens, Kind.QUIT);
            default -> throw new IllegalArgumentException("Unknown command: " + tokens[0] + " (type 'help')");
        };
    }

    public static String help() {
        return """
                Available commands:
                  register [server_host server_port]        register this peer's resource in the ring
                  list [server_host server_port]            list every resource stored in the ring
                  resource <hash> <peer_host> <peer_port>   ask a peer for a resource
                  info                                      show this peer's nickname, port and resource
                  help                                      show this help
                  quit | exit                               leave the network""";
    }

    private static ConsoleCommand sendToServerOrTarget(String[] tokens, Message message, PeerConfig config,
                                                       String usage) {
        if (tokens.length == 1) {
            return new ConsoleCommand(Kind.SEND, message, config.serverHost(), config.serverPort());
        }
        if (tokens.length == 3) {
            return new ConsoleCommand(Kind.SEND, message, tokens[1], parsePort(tokens[2]));
        }
        throw new IllegalArgumentException("Usage: " + usage);
    }

    private static ConsoleCommand parseResource(String[] tokens) {
        if (tokens.length != 4) {
            throw new IllegalArgumentException("Usage: resource <hash> <peer_host> <peer_port>");
        }
        String hash = tokens[1];
        if (!Md5.isValidHex(hash)) {
            throw new IllegalArgumentException("Invalid hash (expected 32 hex characters): " + hash);
        }
        return new ConsoleCommand(Kind.SEND,
                Message.of(MessageType.RESOURCE, hash.toLowerCase(Locale.ROOT)),
                tokens[2],
                parsePort(tokens[3]));
    }

    private static ConsoleCommand noArguments(String[] tokens, Kind kind) {
        if (tokens.length != 1) {
            throw new IllegalArgumentException("Usage: " + tokens[0] + " (no arguments)");
        }
        return local(kind);
    }

    private static ConsoleCommand local(Kind kind) {
        return new ConsoleCommand(kind, null, null, 0);
    }

    private static int parsePort(String value) {
        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + value, e);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port out of range (1-65535): " + value);
        }
        return port;
    }
}
