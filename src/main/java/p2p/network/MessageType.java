package p2p.network;

import java.util.Optional;

/**
 * Every message exchanged between peers and super-nodes.
 *
 * <p>Each type has the keyword that appears as the first token of a packet and the minimum number
 * of arguments that must follow it.</p>
 */
public enum MessageType {

    CREATE("create", 1),
    HEARTBEAT("heartbeat", 1),
    REGISTER("register", 1),
    REGISTER_RING("register_ring", 5),
    LIST("list", 0),
    LIST_RING("list_ring", 4),
    RESOURCE("resource", 1);

    private final String keyword;
    private final int minArgs;

    MessageType(String keyword, int minArgs) {
        this.keyword = keyword;
        this.minArgs = minArgs;
    }

    /** @return the first token of a packet of this type. */
    public String keyword() {
        return keyword;
    }

    /** @return the minimum number of arguments that must follow the keyword. */
    public int minArgs() {
        return minArgs;
    }

    /** Looks up a message type by its keyword (case-sensitive). */
    public static Optional<MessageType> fromKeyword(String keyword) {
        if (keyword == null) {
            return Optional.empty();
        }
        String trimmed = keyword.trim();
        for (MessageType type : values()) {
            if (type.keyword.equals(trimmed)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
