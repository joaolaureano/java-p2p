package p2p.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class ResourceEntryTest {

    private static final String HASH = "d41d8cd98f00b204e9800998ecf8427e";

    @Test
    void wireFormRoundTrips() {
        ResourceEntry entry = new ResourceEntry(HASH, "127.0.0.1", 8080);
        assertEquals(HASH + "@127.0.0.1:8080", entry.toWire());
        assertEquals(entry, ResourceEntry.fromWire(entry.toWire()));
    }

    @Test
    void uppercaseHashIsNormalized() {
        ResourceEntry entry = new ResourceEntry(HASH.toUpperCase(Locale.ROOT), "localhost", 5000);
        assertEquals(HASH, entry.hash());
    }

    @Test
    void malformedWireFormsThrow() {
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(null));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(""));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH + "@localhost"));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH + "@localhost:"));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH + "@localhost:notaport"));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH + "@:5000"));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire("notahash@localhost:5000"));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH + "@localhost:0"));
        assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(HASH + "@localhost:70000"));
    }
}
