package eu.le_tian.iConsoleOS.communication;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BtLogTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private long now = 1_795_200_000_000L;

    @Test
    public void appendsTimestampedLines() throws IOException {
        BtLog log = new BtLog(tmp.getRoot(), 10_000, () -> now);
        log.append("connect to i-CONSOLE+0020");
        now += 1500;
        log.append("multi\nline");
        List<String> lines = log.readTail(10);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0), lines.get(0).endsWith("  connect to i-CONSOLE+0020"));
        assertTrue(lines.get(0).matches("\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d{3}  .*"));
        assertTrue("one line per event", lines.get(1).endsWith("  multi line"));
    }

    @Test
    public void rotatesAndKeepsOnePreviousFile() {
        BtLog log = new BtLog(tmp.getRoot(), 200, () -> now);
        for (int i = 0; i < 100; i++)
            log.append("event " + i);
        File current = new File(tmp.getRoot(), BtLog.FILE_NAME);
        File old = new File(tmp.getRoot(), BtLog.OLD_FILE_NAME);
        assertTrue(current.exists() && old.exists());
        assertTrue("bounded size", current.length() <= 200 + 60 && old.length() <= 200 + 60);
        String[] files = tmp.getRoot().list();
        assertEquals(2, files.length);
    }

    @Test
    public void tailReadsAcrossBothFilesInOrder() {
        BtLog log = new BtLog(tmp.getRoot(), 200, () -> now);
        for (int i = 0; i < 100; i++)
            log.append("event " + i);
        List<String> tail = log.readTail(5);
        assertEquals(5, tail.size());
        for (int i = 0; i < 5; i++)
            assertTrue(tail.get(i), tail.get(i).endsWith("event " + (95 + i)));

        List<String> all = log.readTail(10_000);
        // old file first, then current: the numbers keep increasing across the boundary
        int prev = -1;
        for (String line : all) {
            int n = Integer.parseInt(line.substring(line.lastIndexOf(' ') + 1));
            assertTrue(n > prev);
            prev = n;
        }
    }

    @Test
    public void emptyWhenNothingLogged() {
        assertTrue(new BtLog(tmp.getRoot(), 200, () -> now).readTail(10).isEmpty());
    }
}
