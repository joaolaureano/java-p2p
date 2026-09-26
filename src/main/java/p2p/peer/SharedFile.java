package p2p.peer;

import java.nio.file.Path;

/**
 * One file shared by this peer: its MD5 hash, its display name, its size in bytes and where it lives
 * on disk.
 */
public record SharedFile(String hash, String name, long size, Path path) {

    public SharedFile {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("hash must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0, got " + size);
        }
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
    }
}
