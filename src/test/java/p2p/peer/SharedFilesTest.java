package p2p.peer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedFilesTest {

    @Test
    void addFileDedupesIdenticalContent(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.txt");
        Path b = dir.resolve("b.txt");
        Files.writeString(a, "hello world", StandardCharsets.UTF_8);
        Files.writeString(b, "hello world", StandardCharsets.UTF_8);

        SharedFiles files = new SharedFiles();
        SharedFile first = files.addFile(a);
        SharedFile second = files.addFile(b);

        assertEquals(first.hash(), second.hash());
        assertEquals(1, files.size());
        assertEquals(first, second);
    }

    @Test
    void directoriesAreRejected(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        SharedFiles files = new SharedFiles();
        assertThrows(IllegalArgumentException.class, () -> files.addFile(sub));
    }

    @Test
    void addFolderSkipsHiddenFilesAndSubfolders(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("visible.txt"), "v", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(".hidden"), "h", StandardCharsets.UTF_8);
        Path nested = Files.createDirectory(dir.resolve("nested"));
        Files.writeString(nested.resolve("deep.txt"), "d", StandardCharsets.UTF_8);

        SharedFiles files = new SharedFiles();
        List<SharedFile> added = files.addFolder(dir);

        assertEquals(1, added.size());
        assertEquals("visible.txt", added.get(0).name());
        assertEquals(1, files.size());
    }

    @Test
    void addFolderCreatesMissingFolder(@TempDir Path dir) throws IOException {
        SharedFiles files = new SharedFiles();
        List<SharedFile> added = files.addFolder(dir.resolve("missing"));
        assertEquals(0, added.size());
    }

    @Test
    void readChunkSizes(@TempDir Path dir) throws IOException {
        assertChunks(dir, "empty.bin", new byte[0]);
        assertChunks(dir, "one.bin", new byte[SharedFiles.CHUNK_SIZE]);
        assertChunks(dir, "three.bin", new byte[SharedFiles.CHUNK_SIZE * 2 + 5]);
    }

    private void assertChunks(Path dir, String name, byte[] data) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, data);

        SharedFiles files = new SharedFiles();
        SharedFile shared = files.addFile(file);

        int expectedChunks = SharedFiles.chunkCount(data.length);
        assertEquals(expectedChunks, SharedFiles.chunkCount(shared.size()));

        for (int index = 0; index < expectedChunks; index++) {
            byte[] chunk = files.readChunk(shared.hash(), index);
            long expectedLength = Math.min(SharedFiles.CHUNK_SIZE,
                    shared.size() - (long) index * SharedFiles.CHUNK_SIZE);
            assertEquals(expectedLength, chunk.length);
        }

        assertThrows(IllegalArgumentException.class, () -> files.readChunk(shared.hash(), -1));
        assertThrows(IllegalArgumentException.class, () -> files.readChunk(shared.hash(), expectedChunks));
    }

    @Test
    void unknownHashIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("x.bin");
        Files.write(file, new byte[4]);
        SharedFiles files = new SharedFiles();
        assertThrows(IllegalArgumentException.class, () -> files.readChunk("0".repeat(32), 0));
    }
}
