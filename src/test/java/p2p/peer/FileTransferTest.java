package p2p.peer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import p2p.network.Message;
import p2p.network.MessageType;
import p2p.network.Packet;
import p2p.network.UdpEndpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTransferTest {

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    private final List<UdpEndpoint> endpoints = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (UdpEndpoint endpoint : endpoints) {
            endpoint.close();
        }
        endpoints.clear();
    }

    @Test
    void downloadsLargeRandomFile(@TempDir Path dir) throws Exception {
        byte[] data = new byte[100 * 1024 + 123];
        new Random(1).nextBytes(data);
        Path source = dir.resolve("random.bin");
        Files.write(source, data);

        SharedFiles uploaderFiles = new SharedFiles();
        SharedFile shared = uploaderFiles.addFile(source);

        UdpEndpoint uploader = newEndpoint();
        UdpEndpoint downloaderEndpoint = newEndpoint();
        Path downloads = dir.resolve("downloads");

        startListener(uploader, uploaderFiles, new FileDownloader(uploader, dir.resolve("uploader-downloads")));
        FileDownloader downloader = newDownloader(downloaderEndpoint, downloads);

        Path saved = downloader.download(shared.hash(), LOOPBACK, uploader.localPort());

        assertTrue(Files.exists(saved), "downloaded file should exist");
        assertEquals("random.bin", saved.getFileName().toString());
        assertEquals(-1, Files.mismatch(source, saved));
    }

    @Test
    void downloadsEmptyFile(@TempDir Path dir) throws Exception {
        Path source = dir.resolve("empty.bin");
        Files.write(source, new byte[0]);

        SharedFiles uploaderFiles = new SharedFiles();
        SharedFile shared = uploaderFiles.addFile(source);

        UdpEndpoint uploader = newEndpoint();
        UdpEndpoint downloaderEndpoint = newEndpoint();
        Path downloads = dir.resolve("downloads");

        startListener(uploader, uploaderFiles, new FileDownloader(uploader, dir.resolve("uploader-downloads")));
        FileDownloader downloader = newDownloader(downloaderEndpoint, downloads);

        Path saved = downloader.download(shared.hash(), LOOPBACK, uploader.localPort());

        assertTrue(Files.exists(saved));
        assertEquals(0, Files.size(saved));
    }

    @Test
    void downloadsFileOfExactlyTwoChunks(@TempDir Path dir) throws Exception {
        byte[] data = new byte[SharedFiles.CHUNK_SIZE * 2];
        new Random(2).nextBytes(data);
        Path source = dir.resolve("two.bin");
        Files.write(source, data);

        SharedFiles uploaderFiles = new SharedFiles();
        SharedFile shared = uploaderFiles.addFile(source);

        UdpEndpoint uploader = newEndpoint();
        UdpEndpoint downloaderEndpoint = newEndpoint();
        Path downloads = dir.resolve("downloads");

        startListener(uploader, uploaderFiles, new FileDownloader(uploader, dir.resolve("uploader-downloads")));
        FileDownloader downloader = newDownloader(downloaderEndpoint, downloads);

        Path saved = downloader.download(shared.hash(), LOOPBACK, uploader.localPort());

        assertEquals(2, SharedFiles.chunkCount(data.length));
        assertEquals(-1, Files.mismatch(source, saved));
    }

    @Test
    void unknownHashLeavesNothingBehind(@TempDir Path dir) throws Exception {
        UdpEndpoint uploader = newEndpoint();
        SharedFiles uploaderFiles = new SharedFiles();
        Path downloads = dir.resolve("downloads");

        startListener(uploader, uploaderFiles, new FileDownloader(uploader, dir.resolve("uploader-downloads")));
        UdpEndpoint downloaderEndpoint = newEndpoint();
        FileDownloader downloader = newDownloader(downloaderEndpoint, downloads);

        String hash = "0".repeat(32);

        assertThrows(DownloadException.class, () -> downloader.download(hash, LOOPBACK, uploader.localPort()));
        assertFalse(downloader.isExpecting(hash));

        if (Files.exists(downloads)) {
            try (var stream = Files.list(downloads)) {
                assertEquals(0, stream.count(), "no partial file should be left behind");
            }
        }
    }

    @Test
    void nameCollisionSavesNumberedCopy(@TempDir Path dir) throws Exception {
        Path source = dir.resolve("report.txt");
        Files.write(source, "hello world".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        SharedFiles uploaderFiles = new SharedFiles();
        SharedFile shared = uploaderFiles.addFile(source);

        UdpEndpoint uploader = newEndpoint();
        UdpEndpoint downloaderEndpoint = newEndpoint();
        Path downloads = dir.resolve("downloads");

        startListener(uploader, uploaderFiles, new FileDownloader(uploader, dir.resolve("uploader-downloads")));
        FileDownloader downloader = newDownloader(downloaderEndpoint, downloads);

        Path first = downloader.download(shared.hash(), LOOPBACK, uploader.localPort());
        Path second = downloader.download(shared.hash(), LOOPBACK, uploader.localPort());

        assertEquals("report.txt", first.getFileName().toString());
        assertEquals("report (1).txt", second.getFileName().toString());
        assertEquals(-1, Files.mismatch(first, second));
    }

    @Test
    void flakyUploaderStillSucceeds(@TempDir Path dir) throws Exception {
        byte[] data = new byte[SharedFiles.CHUNK_SIZE * 3 + 17];
        new Random(7).nextBytes(data);
        Path source = dir.resolve("flaky.bin");
        Files.write(source, data);

        SharedFiles uploaderFiles = new SharedFiles();
        SharedFile shared = uploaderFiles.addFile(source);

        UdpEndpoint uploader = newEndpoint();
        Thread flaky = new Thread(() -> runFlakyUploader(uploader, uploaderFiles), "flaky-uploader");
        flaky.setDaemon(true);
        flaky.start();

        UdpEndpoint downloaderEndpoint = newEndpoint();
        Path downloads = dir.resolve("downloads");
        FileDownloader downloader = newDownloader(downloaderEndpoint, downloads);

        Path saved = downloader.download(shared.hash(), LOOPBACK, uploader.localPort());

        assertEquals(-1, Files.mismatch(source, saved));
    }

    private UdpEndpoint newEndpoint() {
        UdpEndpoint endpoint = new UdpEndpoint(0);
        endpoints.add(endpoint);
        return endpoint;
    }

    private FileDownloader newDownloader(UdpEndpoint endpoint, Path downloads) {
        FileDownloader downloader = new FileDownloader(endpoint, downloads);
        startListener(endpoint, new SharedFiles(), downloader);
        return downloader;
    }

    private void startListener(UdpEndpoint endpoint, SharedFiles files, FileDownloader downloader) {
        PrintStream quiet = new PrintStream(OutputStream.nullOutputStream(), true);
        PeerListener listener = new PeerListener(endpoint, files, downloader, entry -> { }, () -> { }, quiet);
        Thread thread = new Thread(listener, "listener-" + endpoint.localPort());
        thread.setDaemon(true);
        thread.start();
    }

    private void runFlakyUploader(UdpEndpoint endpoint, SharedFiles files) {
        int chunkRequests = 0;
        while (!endpoint.isClosed()) {
            Packet packet;
            try {
                Optional<Packet> received = endpoint.receive(Duration.ofMillis(200));
                if (received.isEmpty()) {
                    continue;
                }
                packet = received.get();
            } catch (RuntimeException e) {
                return;
            }

            Message message;
            try {
                message = Message.parse(packet.data());
            } catch (IllegalArgumentException e) {
                continue;
            }

            try {
                if (message.type() == MessageType.META) {
                    String hash = message.arg(0);
                    Optional<SharedFile> file = files.get(hash);
                    Message reply = file
                            .map(f -> Message.of(MessageType.META_OK, f.hash(), Long.toString(f.size()),
                                    Integer.toString(SharedFiles.chunkCount(f.size())),
                                    p2p.dht.ResourceEntry.encodeName(f.name())))
                            .orElseGet(() -> Message.of(MessageType.META_MISSING, hash));
                    endpoint.send(reply, packet.address(), packet.port());
                } else if (message.type() == MessageType.CHUNK) {
                    chunkRequests++;
                    if (chunkRequests % 3 == 0) {
                        continue; // deliberately drop every third request
                    }
                    String hash = message.arg(0);
                    int index = Integer.parseInt(message.arg(1));
                    byte[] body = files.readChunk(hash, index);
                    endpoint.send(Message.withBody(MessageType.CHUNK_DATA, body, hash, Integer.toString(index)),
                            packet.address(), packet.port());
                }
            } catch (RuntimeException | IOException e) {
                // flaky uploader: keep going
            }
        }
    }
}
