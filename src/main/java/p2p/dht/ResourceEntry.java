package p2p.dht;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * One file announced to the DHT and the peer that holds it.
 *
 * @param hash lowercase 32 character MD5 of the file content
 * @param host host where the file can be fetched
 * @param port port where the file can be fetched
 * @param size file size in bytes
 * @param name file name chosen by the sharing peer
 */
public record ResourceEntry(String hash, String host, int port, long size, String name) {

    public ResourceEntry {
        if (hash == null) {
            throw new IllegalArgumentException("hash must not be null");
        }
        hash = hash.trim().toLowerCase(Locale.ROOT);
        if (!Md5.isValidHex(hash)) {
            throw new IllegalArgumentException("Invalid resource hash: '" + hash + "'");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        host = host.trim();
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535 but was " + port);
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative but was " + size);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /** @return the wire form {@code hash,size,host,port,url-encoded-name} (one token, no spaces). */
    public String toWire() {
        return hash + "," + size + "," + host + "," + port + "," + encodeName(name);
    }

    /**
     * Parses the wire form produced by {@link #toWire()}.
     *
     * @throws IllegalArgumentException if {@code wire} is null, blank, malformed or holds invalid values
     */
    public static ResourceEntry fromWire(String wire) {
        if (wire == null || wire.isBlank()) {
            throw new IllegalArgumentException("Resource wire form must not be null or blank");
        }
        String[] parts = wire.split(",", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Malformed resource wire form (expected 5 parts): '" + wire + "'");
        }
        long size;
        int port;
        try {
            size = Long.parseLong(parts[1].trim());
            port = Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in resource wire form: '" + wire + "'", e);
        }
        return new ResourceEntry(parts[0], parts[2], port, size, decodeName(parts[4]));
    }

    /** URL-encodes a file name (UTF-8) so it can travel as one token of a protocol line. */
    public static String encodeName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }

    /**
     * Decodes a name produced by {@link #encodeName(String)}.
     *
     * @throws IllegalArgumentException if the encoded name is malformed
     */
    public static String decodeName(String encoded) {
        Objects.requireNonNull(encoded, "encoded must not be null");
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed encoded name: '" + encoded + "'", e);
        }
    }
}
