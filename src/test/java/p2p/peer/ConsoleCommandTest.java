package p2p.peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import p2p.network.MessageType;

class ConsoleCommandTest {

    private final SharedResource resource = new SharedResource("hello world");
    private final PeerConfig config = new PeerConfig("127.0.0.1", 5000, "alice", 6000);

    private ConsoleCommand parse(String line) {
        return ConsoleCommand.parse(line, config, resource);
    }

    @Test
    void registerWithoutTargetGoesToOwnServer() {
        ConsoleCommand command = parse("register");

        assertEquals(ConsoleCommand.Kind.SEND, command.kind());
        assertEquals(MessageType.REGISTER, command.message().type());
        assertEquals(resource.hash(), command.message().arg(0));
        assertEquals("127.0.0.1", command.targetHost());
        assertEquals(5000, command.targetPort());
    }

    @Test
    void registerWithExplicitTarget() {
        ConsoleCommand command = parse("register 10.0.0.5 7001");

        assertEquals(MessageType.REGISTER, command.message().type());
        assertEquals("10.0.0.5", command.targetHost());
        assertEquals(7001, command.targetPort());
    }

    @Test
    void listWithAndWithoutTarget() {
        ConsoleCommand own = parse("list");
        assertEquals(MessageType.LIST, own.message().type());
        assertEquals(5000, own.targetPort());

        ConsoleCommand other = parse("list 10.0.0.5 7002");
        assertEquals("10.0.0.5", other.targetHost());
        assertEquals(7002, other.targetPort());
    }

    @Test
    void resourceCommandNormalizesTheHash() {
        String upper = resource.hash().toUpperCase(Locale.ROOT);
        ConsoleCommand command = parse("resource " + upper + " 10.0.0.9 8000");

        assertEquals(MessageType.RESOURCE, command.message().type());
        assertEquals(resource.hash(), command.message().arg(0));
        assertEquals("10.0.0.9", command.targetHost());
        assertEquals(8000, command.targetPort());
    }

    @Test
    void localCommands() {
        assertEquals(ConsoleCommand.Kind.INFO, parse("info").kind());
        assertEquals(ConsoleCommand.Kind.HELP, parse("HELP").kind());
        assertEquals(ConsoleCommand.Kind.QUIT, parse("quit").kind());
        assertEquals(ConsoleCommand.Kind.QUIT, parse("exit").kind());
    }

    @Test
    void blankLinesAreEmptyCommands() {
        assertEquals(ConsoleCommand.Kind.EMPTY, parse(null).kind());
        assertEquals(ConsoleCommand.Kind.EMPTY, parse("").kind());
        assertEquals(ConsoleCommand.Kind.EMPTY, parse("   ").kind());
    }

    @Test
    void helpListsEveryCommand() {
        String help = ConsoleCommand.help();
        for (String command : new String[] {"register", "list", "resource", "info", "help", "quit"}) {
            assertTrue(help.contains(command), "help is missing " + command);
        }
    }

    @Test
    void badInputsThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> parse("resource"));
        assertThrows(IllegalArgumentException.class, () -> parse("resource zz 127.0.0.1 5000"));
        assertThrows(IllegalArgumentException.class, () -> parse("resource " + resource.hash() + " host 0"));
        assertThrows(IllegalArgumentException.class, () -> parse("list host notaport"));
        assertThrows(IllegalArgumentException.class, () -> parse("list host"));
        assertThrows(IllegalArgumentException.class, () -> parse("register host 99999"));
        assertThrows(IllegalArgumentException.class, () -> parse("info extra"));
        assertThrows(IllegalArgumentException.class, () -> parse("foo"));
    }
}
