package p2p.dht;

import java.util.Locale;

/**
 * One resource announced to the DHT: the hash of its content and the peer that holds it.
 *
 * @param hash lowercase 32 character MD5 hash
 * @param host host where the resource can be fetched
 * @param port port where the resource can be fetched
 */
public record ResourceEntry(String hash, String host, int port) {

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
    }

    /** @return the wire representation: {@code hash@host:port}. */
    public String toWire() {
        return hash + "@" + host + ":" + port;
    }

    /**
     * Parses the wire representation produced by {@link #toWire()}.
     *
     * @throws IllegalArgumentException if {@code wire} is null, blank or malformed
     */
    public static ResourceEntry fromWire(String wire) {
        if (wire == null || wire.isBlank()) {
            throw new IllegalArgumentException("Resource wire form must not be null or blank");
        }

        int at = wire.indexOf('@');
        int colon = wire.lastIndexOf(':');
        if (at <= 0 || colon <= at + 1 || colon == wire.length() - 1) {
            throw new IllegalArgumentException("Malformed resource wire form: '" + wire + "'");
        }

        String hash = wire.substring(0, at);
        String host = wire.substring(at + 1, colon);
        String portText = wire.substring(colon + 1);

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port in resource wire form: '" + wire + "'", e);
        }

        return new ResourceEntry(hash, host, port);
    }
}
