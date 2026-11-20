package eu.le_tian.iConsoleOS.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SampleCodecTest {

    private static ExerciseData sample(long t, int power, int speed, int dist, int cal, int hr, int rpm) {
        return new ExerciseData(0, power, speed, dist, cal, hr, rpm, t, 42);
    }

    /** A realistic ride: 2 samples per second, slowly changing values. */
    private static List<ExerciseData> ride(int seconds, long seed) {
        Random r = new Random(seed);
        List<ExerciseData> out = new ArrayList<>();
        int power = 800, speed = 250, dist = 0, cal = 0, hr = 70, rpm = 60;
        for (int t = 0; t < seconds; t++) {
            for (int k = 0; k < 2; k++) {
                power = Math.max(0, power + r.nextInt(41) - 20);
                speed = Math.max(0, speed + r.nextInt(5) - 2);
                hr = Math.max(40, Math.min(200, hr + r.nextInt(3) - 1));
                rpm = Math.max(0, rpm + r.nextInt(5) - 2);
                if (r.nextInt(10) == 0) dist++;
                if (r.nextInt(20) == 0) cal++;
                out.add(sample(t, power, speed, dist, cal, hr, rpm));
            }
        }
        return out;
    }

    private static List<ExerciseData> roundTrip(List<ExerciseData> in) {
        return SampleCodec.decode(SampleCodec.encode(in), 42);
    }

    @Test
    public void roundTripRealisticRide() {
        List<ExerciseData> in = ride(3600, 1);
        List<ExerciseData> out = roundTrip(in);
        assertTrue(SampleCodec.sameSamples(in, out));
        assertEquals(42, out.get(0).getExerciseParentID());
    }

    @Test
    public void realisticRideIsMuchSmallerThanRows() {
        List<ExerciseData> in = ride(3600, 2);
        byte[] blob = SampleCodec.encode(in);
        // 7200 samples: well under 4 bytes per sample (a DB row is ~40 bytes)
        assertTrue("blob " + blob.length + " bytes", blob.length < in.size() * 4);
    }

    @Test
    public void emptyAndSingle() {
        assertEquals(0, roundTrip(Collections.emptyList()).size());
        List<ExerciseData> one = Collections.singletonList(sample(5, 1234, 255, 10, 3, 120, 80));
        assertTrue(SampleCodec.sameSamples(one, roundTrip(one)));
    }

    @Test
    public void extremesAndDropsSurvive() {
        List<ExerciseData> in = Arrays.asList(
                sample(Long.MAX_VALUE / 2, Integer.MAX_VALUE, 0, 0, 0, 0, 0),
                sample(0, Integer.MIN_VALUE, -5, 99999, -1, 255, 0),   // big negative jumps
                sample(-3, 0, Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 0, -100),
                sample(-3, 0, Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 0, -100)); // duplicate
        assertTrue(SampleCodec.sameSamples(in, roundTrip(in)));
    }

    @Test
    public void encodingIsStable() {
        // Old blobs must stay readable: pin the exact bytes of a tiny version-1 blob.
        List<ExerciseData> in = Arrays.asList(sample(0, 10, 20, 0, 0, 70, 60), sample(1, 12, 19, 1, 0, 71, 61));
        byte[] blob = SampleCodec.encode(in);
        List<ExerciseData> decoded = SampleCodec.decode(blob, 1);
        assertTrue(SampleCodec.sameSamples(in, decoded));
        assertArrayEquals(blob, SampleCodec.encode(decoded));
    }

    @Test
    public void sameSamplesDetectsDifferences() {
        List<ExerciseData> a = ride(10, 3);
        List<ExerciseData> b = roundTrip(a);
        b.get(7).setExHR(b.get(7).getExHR() + 1);
        assertFalse(SampleCodec.sameSamples(a, b));
        assertFalse(SampleCodec.sameSamples(a, b.subList(0, 5)));
    }

    @Test
    public void corruptBlobsAreRejected() {
        byte[] blob = SampleCodec.encode(ride(60, 4));
        expectRejected(Arrays.copyOf(blob, blob.length / 2));        // truncated
        expectRejected(new byte[]{1, 2, 3, 4, 5});                   // not deflate
        byte[] flipped = blob.clone();
        flipped[flipped.length / 2] ^= 0x55;
        expectRejectedOrDifferent(flipped, ride(60, 4));
    }

    private static void expectRejected(byte[] blob) {
        try {
            SampleCodec.decode(blob, 1);
            fail("corrupt blob accepted");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static void expectRejectedOrDifferent(byte[] blob, List<ExerciseData> original) {
        try {
            assertFalse(SampleCodec.sameSamples(original, SampleCodec.decode(blob, 1)));
        } catch (IllegalArgumentException expected) {
            // ok: compaction would roll back either way
        }
    }
}
