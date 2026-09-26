package p2p.peer;

import p2p.dht.Md5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of the files shared by this peer, keyed by MD5 hash.
 *
 * <p>Files are stored exactly as they are on disk: the same file added twice (or two files with the
 * same content) are de-duplicated by hash.</p>
 */
public final class SharedFiles {

    /** Size of a transfer chunk: the last chunk of a file may be shorter than this. */
    public static final int CHUNK_SIZE = 8192;

    private final ConcurrentHashMap<String, SharedFile> byHash = new ConcurrentHashMap<>();

    /**
     * Shares a single regular, readable file.
     *
     * @return the shared file; if the same content is already shared, the previously stored entry
     */
    public SharedFile addFile(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Not a readable file: null");
        }
        Path absolute = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute) || !Files.isReadable(absolute)) {
            throw new IllegalArgumentException("Not a readable file: " + file);
        }

        String hash = Md5.ofFile(absolute);
        SharedFile existing = byHash.get(hash);
        if (existing != null) {
            return existing;
        }

        long size = Files.size(absolute);
        SharedFile shared = new SharedFile(hash, absolute.getFileName().toString(), size, absolute);
        SharedFile raced = byHash.putIfAbsent(hash, shared);
        return raced != null ? raced : shared;
    }

    /**
     * Shares every regular file directly inside {@code folder} (not recursive). Hidden files
     * (name starting with {@code .}) and sub-directories are skipped. The folder is created if it
     * does not exist yet.
     *
     * @return the files that were added by this call
     */
    public List<SharedFile> addFolder(Path folder) throws IOException {
        if (folder == null) {
            throw new IllegalArgumentException("folder must not be null");
        }
        Path absolute = folder.toAbsolutePath().normalize();
        Files.createDirectories(absolute);

        List<SharedFile> added = new ArrayList<>();
        try (var stream = Files.list(absolute)) {
            for (Path candidate : stream.toList()) {
                String name = candidate.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                added.add(addFile(candidate));
            }
        }
        return added;
    }

    /** Looks up a shared file by hash, ignoring case. */
    public Optional<SharedFile> get(String hash) {
        if (hash == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byHash.get(hash.toLowerCase(Locale.ROOT)));
    }

    /** Every shared file, sorted by name. */
    public List<SharedFile> all() {
        List<SharedFile> list = new ArrayList<>(byHash.values());
        list.sort(Comparator.comparing(SharedFile::name));
        return list;
    }

    public int size() {
        return byHash.size();
    }

    /** Number of {@link #CHUNK_SIZE}-byte chunks needed to hold {@code size} bytes (0 for empty files). */
    public static int chunkCount(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0, got " + size);
        }
        return (int) ((size + CHUNK_SIZE - 1) / CHUNK_SIZE);
    }

    /**
     * Reads one chunk of a shared file.
     *
     * @throws IllegalArgumentException if the hash is unknown or the index is out of range
     */
    public byte[] readChunk(String hash, int index) throws IOException {
        SharedFile file = get(hash).orElseThrow(() -> new IllegalArgumentException("Unknown hash: " + hash));
        int chunks = chunkCount(file.size());
        if (index < 0 || index >= chunks) {
            throw new IllegalArgumentException(
                    "Chunk index out of range: " + index + " (file has " + chunks + " chunks)");
        }

        long offset = (long) index * CHUNK_SIZE;
        int length = (int) Math.min(CHUNK_SIZE, file.size() - offset);

        try (FileChannel channel = FileChannel.open(file.path(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            channel.position(offset);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    break;
                }
            }
            return buffer.array();
        }
    }
}
