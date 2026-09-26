package p2p.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Md5Test {

    @Test
    void knownVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.of(new byte[0]));
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Md5.of("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void fileHashMatchesByteHash(@TempDir Path dir) throws IOException {
        byte[] data = new byte[200_000];
        new Random(42).nextBytes(data);
        Path file = Files.write(dir.resolve("random.bin"), data);

        assertEquals(Md5.of(data), Md5.ofFile(file));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.ofFile(Files.write(dir.resolve("empty"), new byte[0])));
    }

    @Test
    void isValidHex() {
        assertTrue(Md5.isValidHex("d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(Md5.isValidHex("D41D8CD98F00B204E9800998ECF8427E"));
        assertFalse(Md5.isValidHex(null));
        assertFalse(Md5.isValidHex(""));
        assertFalse(Md5.isValidHex("f".repeat(31)));
        assertFalse(Md5.isValidHex("f".repeat(33)));
        assertFalse(Md5.isValidHex("g".repeat(32)));
    }
}
