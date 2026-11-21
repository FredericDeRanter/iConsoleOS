package eu.le_tian.iConsoleOS.communication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HeartRateMonitorTest {

    /** An ANT+ HR page: page number, three page-specific bytes, beat time (2), beat count, HR. */
    private static byte[] page(int pageNumber, int bpm) {
        return new byte[]{(byte) pageNumber, (byte) 0xFF, 0, 0, 0x10, 0x20, 42, (byte) bpm};
    }

    @Test
    public void heartRateIsTheLastByteOfEveryPageType() {
        assertEquals(72, HeartRateMonitor.computedHeartRate(page(0x04, 72)));
        assertEquals(131, HeartRateMonitor.computedHeartRate(page(0x84, 131))); // toggle bit set
        assertEquals("unsigned byte", 200, HeartRateMonitor.computedHeartRate(page(0x00, 200)));
        assertEquals(0, HeartRateMonitor.computedHeartRate(new byte[3]));
        assertEquals(0, HeartRateMonitor.computedHeartRate(null));
    }

    @Test
    public void freshForFiveSeconds() {
        HeartRateMonitor m = new HeartRateMonitor();
        assertEquals("nothing received yet", 0, m.heartRate(1000));
        assertTrue(m.onPage(page(4, 120), 10_000));
        assertEquals(120, m.heartRate(10_000));
        assertEquals(120, m.heartRate(15_000));
        assertEquals("stale", 0, m.heartRate(15_001));
    }

    @Test
    public void invalidPageKeepsThePreviousValue() {
        HeartRateMonitor m = new HeartRateMonitor();
        m.onPage(page(4, 110), 0);
        assertFalse(m.onPage(page(4, 0), 1000));
        assertEquals(110, m.heartRate(1000));
    }

    @Test
    public void clearForgetsTheValue() {
        HeartRateMonitor m = new HeartRateMonitor();
        m.onPage(page(4, 110), 0);
        m.clear();
        assertEquals(0, m.heartRate(0));
    }

    @Test
    public void watchWinsWhenFreshElseBike() {
        assertEquals(125, HeartRateMonitor.choose(125, 98));
        assertEquals(98, HeartRateMonitor.choose(0, 98));
        assertEquals(0, HeartRateMonitor.choose(0, 0));
    }
}
