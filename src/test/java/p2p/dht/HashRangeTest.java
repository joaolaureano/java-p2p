package p2p.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class HashRangeTest {

    private static final String MAX_HASH = "ffffffffffffffffffffffffffffffff";

    @Test
    void rangesAreContiguousAndCoverTheWholeKeySpace() {
        for (int ringSize = 1; ringSize <= 7; ringSize++) {
            HashRange previous = HashRange.forNode(1, ringSize);
            assertEquals(BigInteger.ZERO, previous.min(), "first node must start at 0 (ringSize " + ringSize + ")");

            for (int position = 2; position <= ringSize; position++) {
                HashRange current = HashRange.forNode(position, ringSize);
                assertEquals(previous.max().add(BigInteger.ONE), current.min(),
                        "gap before node " + position + " (ringSize " + ringSize + ")");
                assertTrue(current.min().compareTo(current.max()) <= 0);
                previous = current;
            }

            assertEquals(HashRange.KEY_SPACE.subtract(BigInteger.ONE), previous.max(),
                    "last node must end at 2^128 - 1 (ringSize " + ringSize + ")");
        }
    }

    @Test
    void invalidPositionOrRingSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> HashRange.forNode(0, 3));
        assertThrows(IllegalArgumentException.class, () -> HashRange.forNode(4, 3));
        assertThrows(IllegalArgumentException.class, () -> HashRange.forNode(1, 0));
        assertThrows(IllegalArgumentException.class, () -> HashRange.forNode(1, -1));
    }

    @Test
    void containsChecksTheBoundaries() {
        HashRange first = HashRange.forNode(1, 2);
        HashRange second = HashRange.forNode(2, 2);

        assertTrue(first.contains("0".repeat(32)));
        assertTrue(first.contains("7" + "f".repeat(31)));
        assertFalse(first.contains("8" + "0".repeat(31)));

        assertTrue(second.contains("8" + "0".repeat(31)));
        assertTrue(second.contains(MAX_HASH));
        assertFalse(second.contains("0".repeat(32)));
    }

    @Test
    void containsIsFalseForInvalidHashes() {
        HashRange range = HashRange.forNode(1, 1);
        assertFalse(range.contains(null));
        assertFalse(range.contains(""));
        assertFalse(range.contains("zz"));
        assertFalse(range.contains("f".repeat(31)));
    }

    @Test
    void lastNodeOfThreeOwnsTheMaximumHash() {
        // v1 bug: with 3 nodes the tail of the key space belonged to nobody.
        assertTrue(HashRange.forNode(3, 3).contains(MAX_HASH));
        assertFalse(HashRange.forNode(1, 3).contains(MAX_HASH));
        assertFalse(HashRange.forNode(2, 3).contains(MAX_HASH));
    }
}
