package p2p.peer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import p2p.dht.ResourceEntry;
import p2p.network.UdpEndpoint;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeerConsoleTest {

    @Test
    void garbageAndEofDoNotKillTheConsole(@TempDir Path dir) throws Exception {
        String input = String.join("\n",
                "definitely not a command",
                "",
                "get not-a-hash",
                "list 127.0.0.1 0",
                "info",
                "help");

        Run run = run(input, dir);

        assertTrue(run.output.contains("Error:"), "expected at least one error line, got:\n" + run.output);
        assertTrue(run.output.contains("alice"), "expected the info output, got:\n" + run.output);
    }

    @Test
    void quitStopsTheConsoleImmediately(@TempDir Path dir) throws Exception {
        Run run = run("quit\ninfo\n", dir);

        assertTrue(run.output.contains("Bye."), "expected a goodbye, got:\n" + run.output);
        assertFalse(run.output.contains("Nickname:"),
                "commands after quit should not run, got:\n" + run.output);
    }

    @Test
    void getWithoutKnownOwnerPrintsHint(@TempDir Path dir) throws Exception {
        Run run = run("get " + "a".repeat(32) + "\n", dir);
        assertTrue(run.output.contains("Unknown owner"), "got:\n" + run.output);
    }

    @Test
    void getUsesRememberedListEntry(@TempDir Path dir) throws Exception {
        PeerConfig config = new PeerConfig("127.0.0.1", 9999, "alice", 0,
                dir.resolve("shared"), dir.resolve("downloads"));
        UdpEndpoint endpoint = new UdpEndpoint(0);
        SharedFiles files = new SharedFiles();
        FileDownloader downloader = new FileDownloader(endpoint, config.downloadsDir());

        String hash = "c".repeat(32);
        String input = "get " + hash + "\n";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

        PeerConsole console = new PeerConsole(reader, out, endpoint, config, files, downloader,
                InetAddress.getByName("127.0.0.1"));
        console.rememberListEntry(new ResourceEntry(hash, "127.0.0.1", 12345, 10, "f.bin"));

        assertDoesNotThrow(console::run);
        endpoint.close();

        String text = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Downloading " + hash), "got:\n" + text);
    }

    @Test
    void shareCommandAddsFileAndRegistersIt(@TempDir Path dir) throws Exception {
        Path source = dir.resolve("note.txt");
        java.nio.file.Files.writeString(source, "hello", StandardCharsets.UTF_8);

        Run run = run("share " + source + "\nfiles\n", dir);

        assertTrue(run.output.contains("Sharing note.txt"), "got:\n" + run.output);
        assertTrue(run.output.contains("note.txt"), "got:\n" + run.output);
    }

    private Run run(String input, Path dir) throws Exception {
        PeerConfig config = new PeerConfig("127.0.0.1", 9999, "alice", 0,
                dir.resolve("shared"), dir.resolve("downloads"));
        UdpEndpoint endpoint = new UdpEndpoint(0);
        try {
            SharedFiles files = new SharedFiles();
            FileDownloader downloader = new FileDownloader(endpoint, config.downloadsDir());

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

            PeerConsole console = new PeerConsole(reader, out, endpoint, config, files, downloader,
                    InetAddress.getByName("127.0.0.1"));

            assertDoesNotThrow(console::run);
            return new Run(bytes.toString(StandardCharsets.UTF_8));
        } finally {
            endpoint.close();
        }
    }

    private record Run(String output) {
    }
}
