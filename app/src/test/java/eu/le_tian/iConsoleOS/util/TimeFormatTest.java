package eu.le_tian.iConsoleOS.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeFormatTest {
    @Test
    public void clock() {
        assertEquals("00:00", TimeFormat.clock(0));
        assertEquals("01:05", TimeFormat.clock(65));
        assertEquals("59:59", TimeFormat.clock(3599));
        assertEquals("1:00:00", TimeFormat.clock(3600));
        assertEquals("1:01:01", TimeFormat.clock(3661));
        assertEquals("01:01:01:01", TimeFormat.clock(90061));
        assertEquals("zero hours kept after days", "01:00:05:00", TimeFormat.clock(86400 + 300));
        assertEquals("00:00", TimeFormat.clock(-5));
    }
}
