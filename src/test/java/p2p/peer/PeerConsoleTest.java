package p2p.peer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import p2p.network.UdpEndpoint;

class PeerConsoleTest {

    @Test
    void badInputAndEndOfInputDoNotKillTheConsole() {
        // v1 bug: any of these lines (or EOF) killed the console thread.
        String input = "\ngarbage\nresource\nlist 127.0.0.1 notaport\nhelp\ninfo\n";

        try (UdpEndpoint endpoint = new UdpEndpoint(0)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            PeerConfig config = new PeerConfig("127.0.0.1", 5000, "alice", 0);
            SharedResource resource = new SharedResource("some shared content");

            PeerConsole console = new PeerConsole(
                    new BufferedReader(new StringReader(input)), out, endpoint, config, resource);

            assertDoesNotThrow(console::run);

            String output = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Error: Unknown command: garbage"), output);
            assertTrue(output.contains("Error: Usage: resource"), output);
            assertTrue(output.contains("Error: Invalid port: notaport"), output);
            assertTrue(output.contains("Hash:     " + resource.hash()), output);
        }
    }
}
