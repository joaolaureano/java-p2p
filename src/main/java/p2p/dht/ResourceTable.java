package p2p.dht;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe table of the resources owned by one node.
 *
 * <p>Only hashes inside {@link #range()} are accepted.</p>
 */
public final class ResourceTable {

    private final HashRange range;
    private final Map<String, ResourceEntry> entries = new ConcurrentHashMap<>();

    public ResourceTable(HashRange range) {
        this.range = Objects.requireNonNull(range, "range must not be null");
    }

    /**
     * Stores the entry when its hash is owned by this node.
     *
     * @return {@code true} when the entry was stored, {@code false} when the hash is not owned here
     */
    public boolean storeIfOwned(ResourceEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        if (!range.contains(entry.hash())) {
            return false;
        }
        entries.put(entry.hash(), entry);
        return true;
    }

    /** @return the entry with the given hash, or an empty optional when it is unknown or not owned. */
    public Optional<ResourceEntry> get(String hash) {
        if (!Md5.isValidHex(hash)) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(hash.toLowerCase(Locale.ROOT)));
    }

    /** @return every stored entry, sorted by hash. */
    public List<ResourceEntry> all() {
        return entries.values().stream()
                .sorted(Comparator.comparing(ResourceEntry::hash))
                .toList();
    }

    /** @return the range of hashes owned by this table. */
    public HashRange range() {
        return range;
    }

    /** @return how many entries are stored. */
    public int size() {
        return entries.size();
    }
}
