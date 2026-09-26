package p2p.peer;

import p2p.dht.Md5;
import p2p.dht.ResourceEntry;
import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.UdpEndpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Downloads files from other peers one chunk at a time.
 *
 * <p>Replies arriving on the peer's socket are handed to {@link #deliver(Message)} by the listener
 * thread and routed to the download that is waiting for them. The actual download runs on whatever
 * thread called {@link #download(String, InetAddress, int)}.</p>
 */
public final class FileDownloader {

    /** How long we wait for a single reply before retrying. */
    public static final Duration TIMEOUT = Duration.ofMillis(500);
    /** How many times a request is sent before the download is aborted. */
    public static final int MAX_ATTEMPTS = 5;

    private final UdpEndpoint endpoint;
    private final Path downloadsDir;
    private final ConcurrentHashMap<String, BlockingQueue<Message>> active = new ConcurrentHashMap<>();

    public FileDownloader(UdpEndpoint endpoint, Path downloadsDir) {
        this.endpoint = endpoint;
        this.downloadsDir = downloadsDir.toAbsolutePath().normalize();
    }

    /** Routes an incoming reply to the download waiting for it, if any; otherwise ignores it. */
    public void deliver(Message message) {
        if (message == null || message.args().isEmpty()) {
            return;
        }
        BlockingQueue<Message> queue = active.get(message.arg(0).toLowerCase(Locale.ROOT));
        if (queue != null) {
            queue.offer(message);
        }
    }

    public boolean isExpecting(String hash) {
        return hash != null && active.containsKey(hash.toLowerCase(Locale.ROOT));
    }

    /**
     * Downloads the file identified by {@code hash} from {@code host:port} and returns the path it
     * was saved to (inside the downloads folder).
     *
     * @throws IllegalStateException if another download of the same hash is already running
     * @throws DownloadException     if the peer does not answer, does not have the file, or the
     *                               transfer fails its size or checksum check
     */
    public Path download(String hash, InetAddress host, int port) throws IOException, DownloadException {
        String key = hash.toLowerCase(Locale.ROOT);
        BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
        if (active.putIfAbsent(key, queue) != null) {
            throw new IllegalStateException("Already downloading " + hash);
        }

        Path part = null;
        boolean done = false;
        try {
            Message meta = requestMeta(queue, key, host, port);
            long size = parseSize(meta);
            int chunks = parseChunks(meta);
            if (size < 0 || chunks != SharedFiles.chunkCount(size)) {
                throw new DownloadException("Malformed meta reply from " + describe(host, port));
            }

            String name = sanitizeName(meta.arg(3), key);
            Files.createDirectories(downloadsDir);
            part = downloadsDir.resolve(name + ".part");

            try (OutputStream out = Files.newOutputStream(part, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int index = 0; index < chunks; index++) {
                    long offset = (long) index * SharedFiles.CHUNK_SIZE;
                    int expected = (int) Math.min(SharedFiles.CHUNK_SIZE, size - offset);
                    out.write(fetchChunk(queue, key, host, port, index, chunks, expected));
                }
            }

            long actualSize = Files.size(part);
            if (actualSize != size) {
                throw new DownloadException("Size mismatch: expected " + size + " but got " + actualSize);
            }
            if (!Md5.ofFile(part).equalsIgnoreCase(key)) {
                throw new DownloadException("Checksum mismatch");
            }

            Path target = uniqueName(name);
            Files.move(part, target);
            done = true;
            return target;
        } finally {
            active.remove(key);
            if (!done && part != null) {
                try {
                    Files.deleteIfExists(part);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }
    }

    private Message requestMeta(BlockingQueue<Message> queue, String key, InetAddress host, int port)
            throws DownloadException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            endpoint.send(Message.of(MessageType.META, key), host, port);
            Message reply = poll(queue, TIMEOUT);
            if (reply == null) {
                continue;
            }
            if (reply.type() == MessageType.META_MISSING) {
                throw new DownloadException("Peer does not share " + key);
            }
            if (reply.type() == MessageType.META_OK) {
                return reply;
            }
        }
        throw new DownloadException("No answer from " + describe(host, port));
    }

    private byte[] fetchChunk(BlockingQueue<Message> queue, String key, InetAddress host, int port,
                              int index, int total, int expected) throws DownloadException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            endpoint.send(Message.of(MessageType.CHUNK, key, Integer.toString(index)), host, port);
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (true) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    break;
                }
                Message reply = poll(queue, Duration.ofNanos(remaining));
                if (reply == null) {
                    break;
                }
                if (reply.type() != MessageType.CHUNK_DATA || reply.args().size() < 2) {
                    continue;
                }
                int replyIndex;
                try {
                    replyIndex = Integer.parseInt(reply.arg(1));
                } catch (NumberFormatException e) {
                    continue;
                }
                if (replyIndex != index) {
                    continue;
                }
                byte[] body = reply.body();
                if (body.length != expected) {
                    throw new DownloadException("Chunk " + index + " of " + total + " has wrong size");
                }
                return body;
            }
        }
        throw new DownloadException("Chunk " + index + " of " + total + " timed out");
    }

    private static Message poll(BlockingQueue<Message> queue, Duration timeout) {
        long millis = Math.max(1, timeout.toMillis());
        try {
            return queue.poll(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static long parseSize(Message meta) throws DownloadException {
        try {
            return Long.parseLong(meta.arg(1));
        } catch (RuntimeException e) {
            throw new DownloadException("Malformed meta reply");
        }
    }

    private static int parseChunks(Message meta) throws DownloadException {
        try {
            return Integer.parseInt(meta.arg(2));
        } catch (RuntimeException e) {
            throw new DownloadException("Malformed meta reply");
        }
    }

    private static String sanitizeName(String encodedName, String fallback) {
        String name;
        try {
            name = ResourceEntry.decodeName(encodedName);
        } catch (RuntimeException e) {
            return fallback;
        }
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            Path fileName = Path.of(name).getFileName();
            if (fileName == null) {
                return fallback;
            }
            String clean = fileName.toString();
            if (clean.isBlank() || clean.equals(".") || clean.equals("..")) {
                return fallback;
            }
            return clean;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private Path uniqueName(String name) {
        Path candidate = downloadsDir.resolve(name);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; ; i++) {
            candidate = downloadsDir.resolve(base + " (" + i + ")" + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }

    private static String describe(InetAddress host, int port) {
        return host.getHostAddress() + ":" + port;
    }
}
