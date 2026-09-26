package p2p.peer;

import org.junit.jupiter.api.Test;
import p2p.network.MessageType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsoleCommandTest {

    private static final PeerConfig CONFIG = new PeerConfig("127.0.0.1", 9000, "alice", 8080,
            Path.of("shared"), Path.of("downloads"));

    @Test
    void shareKeepsSpacesInPath() {
        ConsoleCommand command = ConsoleCommand.parse("share /tmp/my file.txt", CONFIG);
        assertEquals(ConsoleCommand.Kind.SHARE, command.kind());
        assertEquals("/tmp/my file.txt", command.argument());
    }

    @Test
    void filesCommand() {
        assertEquals(ConsoleCommand.Kind.FILES, ConsoleCommand.parse("files", CONFIG).kind());
    }

    @Test
    void registerDefaultsToServer() {
        ConsoleCommand command = ConsoleCommand.parse("register", CONFIG);
        assertEquals(ConsoleCommand.Kind.REGISTER_ALL, command.kind());
        assertEquals("127.0.0.1", command.targetHost());
        assertEquals(9000, command.targetPort());
    }

    @Test
    void registerWithTarget() {
        ConsoleCommand command = ConsoleCommand.parse("register example.org 1234", CONFIG);
        assertEquals(ConsoleCommand.Kind.REGISTER_ALL, command.kind());
        assertEquals("example.org", command.targetHost());
        assertEquals(1234, command.targetPort());
    }

    @Test
    void listDefaultsToServer() {
        ConsoleCommand command = ConsoleCommand.parse("list", CONFIG);
        assertEquals(ConsoleCommand.Kind.SEND, command.kind());
        assertEquals(MessageType.LIST, command.message().type());
        assertEquals("127.0.0.1", command.targetHost());
        assertEquals(9000, command.targetPort());
    }

    @Test
    void listWithTarget() {
        ConsoleCommand command = ConsoleCommand.parse("list other.host 4321", CONFIG);
        assertEquals(ConsoleCommand.Kind.SEND, command.kind());
        assertEquals(MessageType.LIST, command.message().type());
        assertEquals("other.host", command.targetHost());
        assertEquals(4321, command.targetPort());
    }

    @Test
    void getByHashOnly() {
        String hash = "a".repeat(32);
        ConsoleCommand command = ConsoleCommand.parse("get " + hash, CONFIG);
        assertEquals(ConsoleCommand.Kind.GET, command.kind());
        assertEquals(hash, command.argument());
        assertNull(command.targetHost());
    }

    @Test
    void getWithTarget() {
        String hash = "b".repeat(32);
        ConsoleCommand command = ConsoleCommand.parse("get " + hash + " host.example 4444", CONFIG);
        assertEquals(ConsoleCommand.Kind.GET, command.kind());
        assertEquals(hash, command.argument());
        assertEquals("host.example", command.targetHost());
        assertEquals(4444, command.targetPort());
    }

    @Test
    void getHashIsLowercased() {
        String upper = "A".repeat(32);
        ConsoleCommand command = ConsoleCommand.parse("get " + upper, CONFIG);
        assertEquals(upper.toLowerCase(java.util.Locale.ROOT), command.argument());
    }

    @Test
    void invalidHashesPortsAndArityThrow() {
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("get nothex", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("get", CONFIG));
        assertThrows(IllegalArgumentException.class,
                () -> ConsoleCommand.parse("get " + "a".repeat(32) + " host", CONFIG));
        assertThrows(IllegalArgumentException.class,
                () -> ConsoleCommand.parse("get " + "a".repeat(32) + " host 0", CONFIG));
        assertThrows(IllegalArgumentException.class,
                () -> ConsoleCommand.parse("get " + "a".repeat(32) + " host 99999", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("register host 99999", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("register host", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("register a b c d", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("list a b c", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("help now", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("nope", CONFIG));
        assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.parse("share", CONFIG));
    }

    @Test
    void blankIsEmpty() {
        assertEquals(ConsoleCommand.Kind.EMPTY, ConsoleCommand.parse("", CONFIG).kind());
        assertEquals(ConsoleCommand.Kind.EMPTY, ConsoleCommand.parse("   ", CONFIG).kind());
        assertEquals(ConsoleCommand.Kind.EMPTY, ConsoleCommand.parse(null, CONFIG).kind());
    }

    @Test
    void quitAndExit() {
        assertEquals(ConsoleCommand.Kind.QUIT, ConsoleCommand.parse("quit", CONFIG).kind());
        assertEquals(ConsoleCommand.Kind.QUIT, ConsoleCommand.parse("exit", CONFIG).kind());
    }

    @Test
    void infoAndHelp() {
        assertEquals(ConsoleCommand.Kind.INFO, ConsoleCommand.parse("info", CONFIG).kind());
        assertEquals(ConsoleCommand.Kind.HELP, ConsoleCommand.parse("help", CONFIG).kind());
    }
}
