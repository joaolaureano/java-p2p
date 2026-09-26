package p2p.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceTableTest {

    private static final String LOW = "0".repeat(32);
    private static final String HIGH = "f".repeat(32);

    @Test
    void storesOnlyOwnedHashes() {
        ResourceTable table = new ResourceTable(HashRange.forNode(1, 2));

        assertTrue(table.storeIfOwned(new ResourceEntry(LOW, "127.0.0.1", 9000, 1L, "f.bin")));
        assertFalse(table.storeIfOwned(new ResourceEntry(HIGH, "127.0.0.1", 9001, 1L, "f.bin")));

        assertEquals(1, table.size());
        assertTrue(table.get(LOW).isPresent());
        assertTrue(table.get(HIGH).isEmpty());
        assertTrue(table.get("not-a-hash").isEmpty());
    }

    @Test
    void allIsSortedByHash() {
        ResourceTable table = new ResourceTable(HashRange.forNode(1, 1));
        String a = "a".repeat(32);
        String b = "b".repeat(32);
        String c = "c".repeat(32);

        table.storeIfOwned(new ResourceEntry(c, "127.0.0.1", 9100, 1L, "f.bin"));
        table.storeIfOwned(new ResourceEntry(a, "127.0.0.1", 9101, 1L, "f.bin"));
        table.storeIfOwned(new ResourceEntry(b, "127.0.0.1", 9102, 1L, "f.bin"));

        assertEquals(List.of(a, b, c), table.all().stream().map(ResourceEntry::hash).toList());
    }

    @Test
    void storingTheSameHashTwiceKeepsTheLatestEntry() {
        ResourceTable table = new ResourceTable(HashRange.forNode(1, 1));
        String hash = "7" + "f".repeat(31);

        table.storeIfOwned(new ResourceEntry(hash, "127.0.0.1", 9200, 1L, "f.bin"));
        table.storeIfOwned(new ResourceEntry(hash, "127.0.0.1", 9201, 1L, "f.bin"));

        assertEquals(1, table.size());
        assertEquals(9201, table.get(hash).orElseThrow().port());
    }
}
