package p2p.network;

import java.util.List;
import java.util.Objects;

/**
 * A parsed network message: a {@link MessageType} plus its arguments.
 *
 * <p>The argument list is immutable; the canonical constructor copies it.</p>
 */
public record Message(MessageType type, List<String> args) {

    public Message {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(args, "args must not be null");
        args = List.copyOf(args);
    }

    /**
     * Parses a raw packet into a message.
     *
     * @throws IllegalArgumentException if the input is null, blank, has an unknown keyword or too few arguments
     */
    public static Message parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Message must not be null or blank");
        }

        String[] tokens = raw.trim().split("\\s+");
        String keyword = tokens[0];

        MessageType type = MessageType.fromKeyword(keyword)
                .orElseThrow(() -> new IllegalArgumentException("Unknown message type: '" + keyword + "'"));

        int argumentCount = tokens.length - 1;
        if (argumentCount < type.minArgs()) {
            throw new IllegalArgumentException("Message '" + type.keyword() + "' requires at least "
                    + type.minArgs() + " argument(s) but got " + argumentCount);
        }

        return new Message(type, List.of(tokens).subList(1, tokens.length));
    }

    /** @return the argument at {@code index}. */
    public String arg(int index) {
        return args.get(index);
    }

    /** @return the wire representation: keyword followed by the arguments separated by single spaces. */
    public String format() {
        if (args.isEmpty()) {
            return type.keyword();
        }
        return type.keyword() + " " + String.join(" ", args);
    }

    /** Builds a message from a type and its arguments. */
    public static Message of(MessageType type, String... args) {
        Objects.requireNonNull(args, "args must not be null");
        return new Message(type, List.of(args));
    }
}
