package p2p.dht;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Utility methods for MD5 hashes used as keys of the distributed hash table.
 */
public final class Md5 {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private Md5() {
        // utility class
    }

    /**
     * Computes the MD5 hash of the UTF-8 encoding of {@code text}.
     *
     * @return a 32 character lowercase hexadecimal string
     */
    public static String hex(String text) {
        Objects.requireNonNull(text, "text must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int value = bytes[i] & 0xFF;
                out[i * 2] = HEX_DIGITS[value >>> 4];
                out[i * 2 + 1] = HEX_DIGITS[value & 0x0F];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available in this JVM", e);
        }
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
}
