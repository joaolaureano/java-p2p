package p2p.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void parsesHeaderWithExtraWhitespace() {
        Message message = Message.parse("  register   abc  12 file.bin ");
        assertEquals(MessageType.REGISTER, message.type());
        assertEquals(List.of("abc", "12", "file.bin"), message.args());
        assertEquals("register abc 12 file.bin", message.header());
        assertEquals(0, message.body().length);
    }

    @Test
    void invalidHeadersThrow() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse((String) null));
        assertThrows(IllegalArgumentException.class, () -> Message.parse((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> Message.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("OK"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("register abc 12"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("chunk abc"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse(new byte[] {'\n', 1, 2}));
    }

    @Test
    void binaryBodyRoundTrips() {
        // 0x00, a newline, 0xFF and an invalid UTF-8 sequence must all survive untouched.
        byte[] body = {0x00, 0x0A, (byte) 0xFF, (byte) 0xC3, 0x28, 0x0A};
        Message original = Message.withBody(MessageType.CHUNK_DATA, body, "abc", "3");

        Message parsed = Message.parse(original.toBytes());

        assertEquals(MessageType.CHUNK_DATA, parsed.type());
        assertEquals(List.of("abc", "3"), parsed.args());
        assertArrayEquals(body, parsed.body());
        assertEquals(original, parsed);
        assertEquals(original.hashCode(), parsed.hashCode());
    }

    @Test
    void messageWithoutBodyIsJustTheHeader() {
        assertArrayEquals("meta abc".getBytes(StandardCharsets.UTF_8),
                Message.of(MessageType.META, "abc").toBytes());
        assertArrayEquals("list".getBytes(StandardCharsets.UTF_8), Message.of(MessageType.LIST).toBytes());
        assertEquals(0, new Message(MessageType.LIST, List.of(), null).body().length);
    }

    @Test
    void equalsComparesBodies() {
        Message a = Message.withBody(MessageType.CHUNK_DATA, new byte[] {1, 2, 3}, "h", "0");
        Message b = Message.withBody(MessageType.CHUNK_DATA, new byte[] {1, 2, 3}, "h", "0");
        Message c = Message.withBody(MessageType.CHUNK_DATA, new byte[] {1, 2, 4}, "h", "0");

        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void argumentsAreImmutable() {
        Message message = Message.parse("meta abc");
        assertThrows(UnsupportedOperationException.class, () -> message.args().add("x"));
    }
}
