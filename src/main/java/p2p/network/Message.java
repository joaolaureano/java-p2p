package p2p.network;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A parsed network message: a {@link MessageType}, its text arguments and an optional binary body.
 *
 * <p>On the wire a message is a UTF-8 header line, optionally followed by {@code 0x0A} and the raw body
 * bytes. The body may contain any byte (including {@code 0x0A}), so only the first {@code 0x0A} is the
 * separator.</p>
 *
 * <p>The argument list is copied and immutable. The body array is stored as given (no copy), because
 * protocol code treats it as read-only and copying every 8 KiB chunk would waste memory.</p>
 */
public record Message(MessageType type, List<String> args, byte[] body) {

    public Message {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(args, "args must not be null");
        args = List.copyOf(args);
        body = body == null ? new byte[0] : body;
    }

    /**
     * Parses a raw datagram: everything before the first {@code 0x0A} is the header, everything after
     * it is the body.
     *
     * @throws IllegalArgumentException if the data is null, the header is blank, the keyword is unknown
     *                                  or there are too few arguments
     */
    public static Message parse(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Message data must not be null");
        }
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                String header = new String(data, 0, i, StandardCharsets.UTF_8);
                return parseHeader(header, Arrays.copyOfRange(data, i + 1, data.length));
            }
        }
        return parseHeader(new String(data, StandardCharsets.UTF_8), new byte[0]);
    }

    /**
     * Parses a header line without a body.
     *
     * @throws IllegalArgumentException if the input is null, blank, has an unknown keyword or too few arguments
     */
    public static Message parse(String header) {
        return parseHeader(header, new byte[0]);
    }

    private static Message parseHeader(String header, byte[] body) {
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Message must not be null or blank");
        }

        String[] tokens = header.trim().split("\\s+");
        String keyword = tokens[0];

        MessageType type = MessageType.fromKeyword(keyword)
                .orElseThrow(() -> new IllegalArgumentException("Unknown message type: '" + keyword + "'"));

        int argumentCount = tokens.length - 1;
        if (argumentCount < type.minArgs()) {
            throw new IllegalArgumentException("Message '" + type.keyword() + "' requires at least "
                    + type.minArgs() + " argument(s) but got " + argumentCount);
        }

        return new Message(type, List.of(tokens).subList(1, tokens.length), body);
    }

    /** @return the argument at {@code index}. */
    public String arg(int index) {
        return args.get(index);
    }

    /** @return the header line: keyword followed by the arguments separated by single spaces. */
    public String header() {
        if (args.isEmpty()) {
            return type.keyword();
        }
        return type.keyword() + " " + String.join(" ", args);
    }

    /** @return the wire bytes: the header, plus {@code 0x0A} and the body when the body is not empty. */
    public byte[] toBytes() {
        byte[] headerBytes = header().getBytes(StandardCharsets.UTF_8);
        if (body.length == 0) {
            return headerBytes;
        }
        byte[] out = new byte[headerBytes.length + 1 + body.length];
        System.arraycopy(headerBytes, 0, out, 0, headerBytes.length);
        out[headerBytes.length] = '\n';
        System.arraycopy(body, 0, out, headerBytes.length + 1, body.length);
        return out;
    }

    /** Builds a message without a body. */
    public static Message of(MessageType type, String... args) {
        Objects.requireNonNull(args, "args must not be null");
        return new Message(type, List.of(args), new byte[0]);
    }

    /** Builds a message carrying a binary body. */
    public static Message withBody(MessageType type, byte[] body, String... args) {
        Objects.requireNonNull(args, "args must not be null");
        return new Message(type, List.of(args), body);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Message other)) {
            return false;
        }
        return type == other.type && args.equals(other.args) && Arrays.equals(body, other.body);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(type, args) + Arrays.hashCode(body);
    }

    @Override
    public String toString() {
        return "Message[" + header() + (body.length == 0 ? "" : " +" + body.length + "B") + "]";
    }
}
