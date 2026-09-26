package p2p.peer;

import p2p.dht.Md5;

import java.util.Objects;

/**
 * Content a peer is willing to share, plus its MD5 hash (computed once).
 */
public final class SharedResource {

    private final String content;
    private final String hash;

    public SharedResource(String content) {
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.hash = Md5.hex(content);
    }

    public String content() {
        return content;
    }

    public String hash() {
        return hash;
    }

    /** Case-insensitive comparison against this resource's hash. */
    public boolean matches(String hash) {
        return hash != null && this.hash.equalsIgnoreCase(hash);
    }

    @Override
    public String toString() {
        return content + " [" + hash + "]";
    }
}
