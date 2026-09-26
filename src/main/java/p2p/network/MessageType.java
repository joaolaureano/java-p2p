package p2p.network;

import java.util.Optional;

/**
 * Every message exchanged between peers and super-nodes.
 *
 * <p>Each type has the keyword that appears as the first token of a packet and the minimum number of
 * arguments that must follow it.</p>
 */
public enum MessageType {

    /** Announces a nickname to a super-node. */
    CREATE("create", 1),
    /** Periodic keep-alive sent by peers. */
    HEARTBEAT("heartbeat", 1),
    /** Announces a shared file: {@code hash size encoded_name}. */
    REGISTER("register", 3),
    /** Forwards a registration around the ring. */
    REGISTER_RING("register_ring", 7),
    /** Asks a super-node for the resources of the whole ring. */
    LIST("list", 0),
    /** Forwards a list request around the ring. */
    LIST_RING("list_ring", 4),
    /** Asks a peer for the metadata of a shared file. */
    META("meta", 1),
    /** Metadata answer: {@code hash size chunks encoded_name}. */
    META_OK("meta_ok", 4),
    /** The requested hash is not shared by that peer. */
    META_MISSING("meta_missing", 1),
    /** Asks a peer for one chunk: {@code hash index}. */
    CHUNK("chunk", 2),
    /** One chunk: {@code hash index} header plus the raw bytes as body. */
    CHUNK_DATA("chunk_data", 2);

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
