package p2p.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class Md5Test {

    @Test
    void emptyStringHasTheKnownHash() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.hex(""));
    }

    @Test
    void hashIsAlways32LowercaseHexCharacters() {
        for (String input : List.of("", "a", "hello world", "ação", "x".repeat(500))) {
            String hash = Md5.hex(input);
            assertEquals(32, hash.length(), "unexpected length for: " + input);
            assertEquals(hash.toLowerCase(Locale.ROOT), hash);
            assertTrue(Md5.isValidHex(hash));
        }
    }

    @Test
    void isValidHexAcceptsBothCases() {
        assertTrue(Md5.isValidHex("d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(Md5.isValidHex("D41D8CD98F00B204E9800998ECF8427E"));
    }

    @Test
    void isValidHexRejectsEverythingElse() {
        assertFalse(Md5.isValidHex(null));
        assertFalse(Md5.isValidHex(""));
        assertFalse(Md5.isValidHex("0".repeat(31)));
        assertFalse(Md5.isValidHex("0".repeat(33)));
        assertFalse(Md5.isValidHex("g".repeat(32)));
        assertFalse(Md5.isValidHex("d41d8cd98f00b204e9800998ecf8427 "));
    }
}
