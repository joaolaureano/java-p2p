package p2p.dht;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Utility methods for MD5 hashes used as keys of the distributed hash table.
 */
public final class Md5 {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final int BUFFER_SIZE = 64 * 1024;

    private Md5() {
        // utility class
    }

    /** @return the 32 character lowercase hexadecimal MD5 of {@code data}. */
    public static String of(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        MessageDigest digest = newDigest();
        digest.update(data);
        return toHex(digest.digest());
    }

    /**
     * Computes the MD5 of a file's contents, reading it in 64 KiB blocks.
     *
     * @return a 32 character lowercase hexadecimal string
     * @throws IOException if the file cannot be read
     */
    public static String ofFile(Path file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        MessageDigest digest = newDigest();
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (in.read(buffer) != -1) {
                // DigestInputStream updates the digest while reading
            }
        }
        return toHex(digest.digest());
    }

    /** @return {@code true} when {@code s} is exactly 32 hexadecimal characters (either case). */
    public static boolean isValidHex(String s) {
        if (s == null || s.length() != 32) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available in this JVM", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            out[i * 2] = HEX_DIGITS[value >>> 4];
            out[i * 2 + 1] = HEX_DIGITS[value & 0x0F];
        }
        return new String(out);
    }
}
