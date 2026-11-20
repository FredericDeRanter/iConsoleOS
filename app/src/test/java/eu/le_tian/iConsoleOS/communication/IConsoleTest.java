package eu.le_tian.iConsoleOS.communication;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the iConsole serial protocol state machine, driven against a fake bike
 * (queue-backed input stream + fake clock), so no Bluetooth or sleeping is needed.
 */
public class IConsoleTest {

    // Expected outgoing packets (last byte = sum of the preceding bytes & 0xFF)
    private static final String PING = "f0a0010192";
    private static final String INIT_A0 = "f0a0020294";
    private static final String STATUS = "f0a1010193";
    private static final String INIT_A3 = "f0a301010196";
    private static final String INIT_A4 = "f0a4010101010101010101010101a0";
    private static final String START = "f0a501010299";
    private static final String STOP = "f0a50101049b";
    private static final String READ = "f0a2010194";

    private FakeInputStream in;
    private ByteArrayOutputStream out;
    private long now;
    private final List<byte[]> writes = new ArrayList<>();
    private final List<byte[]> reads = new ArrayList<>();
    private final List<IConsole.Data> data = new ArrayList<>();
    private final List<Exception> errors = new ArrayList<>();
    private IConsole console;

    /** Values the fake bike reports in every READ reply. */
    private int[] bikeValues = {0, 0, 12, 34, 55, 75, 120, 5, 400, 1520, 6};

    @Before
    public void setUp() {
        in = new FakeInputStream();
        out = new ByteArrayOutputStream();
        now = 1_000_000L;
        console = new IConsole(in, out,
                new IConsole.DataListener() {
                    @Override
                    public void onData(IConsole.Data d) {
                        data.add(d);
                    }

                    @Override
                    public void onError(Exception e) {
                        errors.add(e);
                    }
                },
                new IConsole.DebugListener() {
                    @Override
                    public void onRead(byte[] bytes) {
                        reads.add(bytes);
                    }

                    @Override
                    public void onWrite(byte[] bytes) {
                        writes.add(bytes);
                    }
                },
                () -> now);
    }

    // ---------------------------------------------------------------- handshake

    @Test
    public void pingsUntilStarted() {
        for (int i = 0; i < 3; i++) {
            assertEquals(PING, hex(sendStep()));
            ackLast();
        }
        assertTrue(errors.isEmpty());
    }

    @Test
    public void startRunsFullInitSequenceThenPollsData() {
        console.start();
        List<String> sent = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            sent.add(hex(sendStep()));
            ackLast();
        }
        assertEquals(List.of(PING, INIT_A0, STATUS, PING, INIT_A3, INIT_A4, START, READ, READ), sent);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void startAfterIdlePingingJoinsInitSequence() {
        sendStep();
        ackLast();
        console.start();
        assertEquals(PING, hex(sendStep()));
        ackLast();
        assertEquals(INIT_A0, hex(sendStep()));
    }

    @Test
    public void everyPacketSentHasValidChecksum() {
        console.start();
        runUntilReading();
        for (int level = 1; level <= 32; level++) {
            assertTrue(console.setLevel(level));
            sendStep();
            ackLast();
            sendStep(); // back to READ
            ackLast();
        }
        console.stop();
        sendStep();
        ackLast();
        sendStep();

        assertFalse(writes.isEmpty());
        for (byte[] p : writes) {
            int sum = 0;
            for (int i = 0; i < p.length - 1; i++) sum += p[i] & 0xFF;
            assertEquals("checksum of " + hex(p), sum & 0xFF, p[p.length - 1] & 0xFF);
        }
    }

    // ---------------------------------------------------------------- data

    @Test
    public void readReplyIsDecodedAndDeliveredToListener() {
        console.start();
        runUntilReading();

        bikeValues = new int[]{0, 1, 2, 3, 256, 87, 1234, 98, 142, 2499, 15};
        sendStep();
        ackLast();

        IConsole.Data d = data.get(data.size() - 1);
        assertEquals(((1 * 60) + 2) * 60 + 3, d.mTime);
        assertEquals(256, d.mSpeed10);
        assertEquals(87, d.mRPM);
        assertEquals(1234, d.mDistance10);
        assertEquals(98, d.mCalories);
        assertEquals(142, d.mHF);
        assertEquals(2499, d.mPower10);
        assertEquals(15, d.mLevel);
    }

    @Test
    public void onlyReadRepliesProduceData() {
        console.start();
        String last;
        do { // PING .. START acks must not produce Data
            last = hex(sendStep());
            ackLast();
        } while (!last.equals(START));
        assertEquals(0, data.size());
        assertEquals(READ, hex(sendStep()));
        ackLast();
        assertEquals(1, data.size());
    }

    @Test
    public void decodesFrameWithDays() {
        byte[] frame = dataFrame(new int[]{2, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0});
        IConsole.Data d = console.new Data(frame);
        assertEquals(((2L * 24 + 3) * 60 + 4) * 60 + 5, d.mTime);
    }

    @Test
    public void timeStrFormatting() {
        assertEquals("00:00", dataAt(0).getTimeStr());
        assertEquals("01:05", dataAt(65).getTimeStr());
        assertEquals("59:59", dataAt(3599).getTimeStr());
        assertEquals("1:01:01", dataAt(3661).getTimeStr());
        assertEquals("01:01:01:01", dataAt(90061).getTimeStr());
        assertEquals("01:00:05:00", dataAt(86400 + 300).getTimeStr()); // zero hours kept after days
    }

    // ---------------------------------------------------------------- set level

    @Test
    public void setLevelIsRejectedBeforeReading() {
        assertFalse(console.setLevel(5));
        console.start();
        sendStep();
        ackLast();
        assertFalse(console.setLevel(5)); // in A0, not READ
    }

    @Test
    public void setLevelSendsLevelPacketThenResumesReading() {
        console.start();
        runUntilReading();
        sendStep(); // a READ, so current state is READ
        ackLast();

        assertTrue(console.setLevel(7));
        sendStep(); // still finishing current READ cycle
        ackLast();
        assertEquals("f0a6010108a0", hex(sendStep()));
        ackLast();
        assertEquals(READ, hex(sendStep()));
        assertTrue(errors.isEmpty());
    }

    // ---------------------------------------------------------------- stop

    @Test
    public void stopSendsStopThenFallsBackToPing() {
        console.start();
        runUntilReading();
        console.stop();
        sendStep(); // the pending READ
        ackLast();
        assertEquals(STOP, hex(sendStep()));
        ackLast();
        assertEquals(PING, hex(sendStep()));
        ackLast();
        assertEquals(PING, hex(sendStep()));
    }

    // ---------------------------------------------------------------- timing

    @Test
    public void sendsAreThrottledTo200msBeforeReading() {
        console.processIO();
        assertEquals(1, writes.size());
        ackLast();

        now += 199;
        console.processIO();
        assertEquals("throttled", 1, writes.size());
        now += 1;
        console.processIO();
        assertEquals(2, writes.size());
    }

    @Test
    public void sendsAreThrottledTo500msWhileReading() {
        console.start();
        runUntilReading();
        sendStep(); // READ sent, ack moves current state to READ
        ackLast();
        int before = writes.size();

        now += 499;
        console.processIO();
        assertEquals("throttled", before, writes.size());
        now += 1;
        console.processIO();
        assertEquals(before + 1, writes.size());
    }

    @Test
    public void missingAckTimesOutAfter10sAndResends() {
        console.start();
        assertEquals(PING, hex(sendStep()));

        now += 10_000;
        console.processIO();
        assertEquals("still waiting at exactly 10s", 1, writes.size());

        now += 1;
        console.processIO(); // times out, clears wait flag
        console.processIO(); // resends
        assertEquals(2, writes.size());
        assertEquals(PING, hex(writes.get(1)));
        assertTrue(errors.isEmpty());
    }

    // ---------------------------------------------------------------- input handling

    @Test
    public void partialAckIsHeldUntilComplete() {
        console.start();
        sendStep();
        byte[] ack = replyFor(writes.get(0));
        in.enqueue(slice(ack, 0, 3));
        console.processIO();
        assertTrue(reads.isEmpty());

        in.enqueue(slice(ack, 3, ack.length));
        console.processIO();
        assertEquals(1, reads.size());
        assertEquals(INIT_A0, hex(sendStep()));
    }

    @Test
    public void staleInputIsFlushedBeforeSending() {
        in.enqueue(new byte[]{1, 2, 3, 4, 5, 6, 7});
        sendStep();
        assertEquals(0, in.available());
    }

    @Test
    public void wrongAckReportsError() {
        console.start();
        sendStep();
        in.enqueue(new byte[]{(byte) 0xf0, (byte) 0xb5, 1, 1, 0});
        assertFalse(console.processIO());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof IOException);
    }

    @Test
    public void wrongStartByteReportsError() {
        sendStep();
        in.enqueue(new byte[]{(byte) 0xf1, (byte) 0xb0, 1, 1, 0});
        assertFalse(console.processIO());
        assertEquals(1, errors.size());
    }

    // ---------------------------------------------------------------- hex helpers

    @Test
    public void hexHelpersRoundTrip() {
        byte[] bytes = {(byte) 0xf0, 0x00, 0x7f, (byte) 0x80, (byte) 0xff};
        assertEquals("f0007f80ff", IConsole.byteArrayToHex(bytes));
        assertArrayEquals(bytes, IConsole.hexStringToByteArray("f0007f80ff"));
        assertArrayEquals(bytes, IConsole.hexStringToByteArray("F0007F80FF"));
    }

    // ================================================================ helpers

    /** Advances the clock past any throttle and lets the console send its next packet. */
    private byte[] sendStep() {
        now += 1000;
        int before = writes.size();
        assertTrue(console.processIO());
        assertEquals("expected a packet to be sent", before + 1, writes.size());
        return writes.get(writes.size() - 1);
    }

    /** Queues the bike's reply to the last packet and lets the console consume it. */
    private void ackLast() {
        in.enqueue(replyFor(writes.get(writes.size() - 1)));
        assertTrue(console.processIO());
    }

    /** Drives start() through to the first READ being sent and acknowledged. */
    private void runUntilReading() {
        String last;
        int guard = 0;
        do {
            last = hex(sendStep());
            ackLast();
            assertTrue("never reached READ", ++guard < 20);
        } while (!last.equals(READ));
    }

    /** What a real iConsole answers to a given command. */
    private byte[] replyFor(byte[] sent) {
        String s = hex(sent);
        if (s.equals(READ)) return dataFrame(bikeValues);
        byte[] reply;
        if (s.equals(INIT_A0)) {
            reply = new byte[6];
            reply[1] = (byte) 0xb7;
        } else if (s.equals(STATUS)) {
            reply = new byte[6];
            reply[1] = (byte) 0xb1;
        } else {
            reply = sent.clone();
            reply[1] = (byte) (0xb0 | (sent[1] & 0x0f));
        }
        reply[0] = (byte) 0xf0;
        return reply;
    }

    /**
     * Builds a 21-byte READ reply. values = {day, hour, min, sec, speed10, rpm, distance10,
     * calories, hf, power10, level}; each byte on the wire is value+1, two-digit fields are base 100.
     */
    private static byte[] dataFrame(int[] v) {
        byte[] f = new byte[21];
        f[0] = (byte) 0xf0;
        f[1] = (byte) 0xb2;
        for (int i = 0; i < 4; i++) f[2 + i] = (byte) (v[i] + 1);
        for (int i = 0; i < 6; i++) {
            f[6 + 2 * i] = (byte) (v[4 + i] / 100 + 1);
            f[7 + 2 * i] = (byte) (v[4 + i] % 100 + 1);
        }
        f[18] = (byte) (v[10] + 1);
        return f;
    }

    private IConsole.Data dataAt(long seconds) {
        return console.new Data(seconds, 0, 0, 0, 0, 0, 0, 0);
    }

    private static byte[] slice(byte[] a, int from, int to) {
        byte[] r = new byte[to - from];
        System.arraycopy(a, from, r, 0, r.length);
        return r;
    }

    private static String hex(byte[] b) {
        return IConsole.byteArrayToHex(b);
    }

    /** InputStream backed by a queue; available() reports what has been enqueued. */
    private static class FakeInputStream extends InputStream {
        private final ArrayDeque<Byte> queue = new ArrayDeque<>();

        void enqueue(byte[] bytes) {
            for (byte b : bytes) queue.add(b);
        }

        @Override
        public int read() {
            Byte b = queue.poll();
            return b == null ? -1 : b & 0xFF;
        }

        @Override
        public int available() {
            return queue.size();
        }
    }
}
