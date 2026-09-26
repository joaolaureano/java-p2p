package p2p.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void parsesAValidMessageWithExtraWhitespace() {
        Message message = Message.parse("  create   alice  ");
        assertEquals(MessageType.CREATE, message.type());
        assertEquals(List.of("alice"), message.args());
        assertEquals("alice", message.arg(0));
    }

    @Test
    void parsesAKeywordWithoutArguments() {
        Message message = Message.parse("list");
        assertEquals(MessageType.LIST, message.type());
        assertEquals(List.of(), message.args());
    }

    @Test
    void formatRoundTrips() {
        Message message = Message.of(MessageType.REGISTER_RING, "abc", "127.0.0.1", "5000", "127.0.0.1", "5001");
        assertEquals("register_ring abc 127.0.0.1 5000 127.0.0.1 5001", message.format());
        assertEquals(message, Message.parse(message.format()));
        assertEquals("list", Message.of(MessageType.LIST).format());
    }

    @Test
    void blankUnknownAndTooFewArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Message.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("garbage"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("create"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("register"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("register_ring a b c d"));
    }

    @Test
    void argumentsAreCopiedImmutably() {
        List<String> source = new ArrayList<>(List.of("alice"));
        Message message = new Message(MessageType.CREATE, source);
        source.add("bob");

        assertEquals(List.of("alice"), message.args());
        assertThrows(UnsupportedOperationException.class, () -> message.args().add("carol"));
    }
}
