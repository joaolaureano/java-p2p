package p2p.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ResourceEntryTest {

    private static final String HASH = "d41d8cd98f00b204e9800998ecf8427e";

    @Test
    void wireFormRoundTripsAnyFileName() {
        for (String name : List.of("report final.pdf", "a,b,c.txt", "ação.png", "100% done.zip")) {
            ResourceEntry entry = new ResourceEntry(HASH, "127.0.0.1", 9000, 1234L, name);
            assertEquals(entry, ResourceEntry.fromWire(entry.toWire()));
        }
    }

    @Test
    void wireFormIsASingleToken() {
        ResourceEntry entry = new ResourceEntry(HASH, "10.0.0.1", 9100, 7L, "a b.txt");
        assertEquals(HASH + ",7,10.0.0.1,9100,a+b.txt", entry.toWire());
    }

    @Test
    void uppercaseHashIsNormalized() {
        ResourceEntry entry = new ResourceEntry(HASH.toUpperCase(Locale.ROOT), "host", 9000, 1L, "f.bin");
        assertEquals(HASH, entry.hash());
    }

    @Test
    void malformedWireFormsThrow() {
        for (String wire : new String[] {
                null, "", "  ",
                HASH + ",1,host,9000",
                HASH + ",x,host,9000,f.bin",
                HASH + ",-1,host,9000,f.bin",
                HASH + ",1,host,0,f.bin",
                HASH + ",1,host,70000,f.bin",
                "nope,1,host,9000,f.bin",
                HASH + ",1, ,9000,f.bin",
                HASH + ",1,host,9000,",
                HASH + ",1,host,9000,%zz"}) {
            assertThrows(IllegalArgumentException.class, () -> ResourceEntry.fromWire(wire), String.valueOf(wire));
        }
    }
}
