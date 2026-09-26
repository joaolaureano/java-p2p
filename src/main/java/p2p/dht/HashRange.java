package p2p.dht;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Inclusive range of MD5 hash values owned by one node of the ring.
 *
 * @param min inclusive lower bound
 * @param max inclusive upper bound
 */
public record HashRange(BigInteger min, BigInteger max) {

    /** Total number of distinct MD5 values: 2^128. */
    public static final BigInteger KEY_SPACE = BigInteger.TWO.pow(128);

    public HashRange {
        Objects.requireNonNull(min, "min must not be null");
        Objects.requireNonNull(max, "max must not be null");
    }

    /**
     * Computes the slice of the hash space owned by one node.
     *
     * <p>The space is divided in {@code ringSize} equal slices. The last node owns everything that is
     * left over, so the whole space from 0 to 2^128 - 1 is always covered with no gap.</p>
     *
     * @param position 1-based position of the node in the ring
     * @param ringSize number of nodes in the ring
     * @throws IllegalArgumentException if {@code ringSize < 1} or {@code position} is out of range
     */
    public static HashRange forNode(int position, int ringSize) {
        if (ringSize < 1) {
            throw new IllegalArgumentException("ringSize must be at least 1 but was " + ringSize);
        }
        if (position < 1 || position > ringSize) {
            throw new IllegalArgumentException(
                    "position must be between 1 and " + ringSize + " but was " + position);
        }

        BigInteger slice = KEY_SPACE.divide(BigInteger.valueOf(ringSize));
        BigInteger min = slice.multiply(BigInteger.valueOf(position - 1L));
        BigInteger max = position == ringSize
                ? KEY_SPACE.subtract(BigInteger.ONE)
                : slice.multiply(BigInteger.valueOf(position)).subtract(BigInteger.ONE);
        return new HashRange(min, max);
    }

    /**
     * @return {@code true} when the given hexadecimal hash falls inside this range; {@code false} for
     *         anything that is not a valid 32 character hexadecimal hash
     */
    public boolean contains(String hexHash) {
        if (!Md5.isValidHex(hexHash)) {
            return false;
        }
        BigInteger value = new BigInteger(hexHash, 16);
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    @Override
    public String toString() {
        return "HashRange[" + toHex(min) + " .. " + toHex(max) + "]";
    }

    private static String toHex(BigInteger value) {
        String hex = value.toString(16);
        return "0".repeat(Math.max(0, 32 - hex.length())) + hex;
    }
}
