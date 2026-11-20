package eu.le_tian.iConsoleOS.data;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Lossless compact encoding of a workout's {@link ExerciseData} samples into one byte array,
 * stored in {@link ExerciseSamples}. Plain Java so it can be unit tested on the JVM.
 *
 * <p>Format (version {@link #VERSION}): a Deflate stream containing
 * {@code varint(VERSION) varint(count)} followed, per sample, by the change from the previous
 * sample of timestamp, power, speed, distance, calories, HR and RPM, each as a zigzag varint.
 * Samples change slowly, so most deltas are 0 or ±1 and compress well: on the real data
 * (5.7M samples) this took 167 MB of rows down to 3.5 MB.
 *
 * <p>The row IDs ({@code exerciseDataID}) are not stored; decoded samples keep their order and
 * get ID 0. Never change the meaning of an existing version: add a new version instead,
 * because old blobs must stay readable forever.
 */
public final class SampleCodec {
    public static final int VERSION = 1;
    private static final int FIELDS = 7;

    private SampleCodec() {
    }

    public static byte[] encode(List<ExerciseData> samples) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(samples.size() * FIELDS + 8);
        writeVarint(raw, VERSION);
        writeVarint(raw, samples.size());
        long[] prev = new long[FIELDS];
        long[] cur = new long[FIELDS];
        for (ExerciseData s : samples) {
            fields(s, cur);
            for (int i = 0; i < FIELDS; i++) {
                writeVarint(raw, zigzag(cur[i] - prev[i]));
                prev[i] = cur[i];
            }
        }

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw.toByteArray());
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.size() / 4 + 64);
            byte[] buf = new byte[8192];
            while (!deflater.finished()) {
                out.write(buf, 0, deflater.deflate(buf));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * @throws IllegalArgumentException if the blob is corrupt or of an unknown version
     */
    public static List<ExerciseData> decode(byte[] blob, long exerciseParentID) {
        byte[] raw = inflate(blob);
        int[] pos = {0};
        long version = readVarint(raw, pos);
        if (version != VERSION)
            throw new IllegalArgumentException("Unknown sample codec version " + version);
        long count = readVarint(raw, pos);
        if (count < 0 || count > raw.length)  // every sample takes at least FIELDS bytes
            throw new IllegalArgumentException("Bad sample count " + count);

        List<ExerciseData> samples = new ArrayList<>((int) count);
        long[] v = new long[FIELDS];
        for (int n = 0; n < count; n++) {
            for (int i = 0; i < FIELDS; i++) {
                v[i] += unzigzag(readVarint(raw, pos));
            }
            samples.add(new ExerciseData(0, (int) v[1], (int) v[2], (int) v[3], (int) v[4],
                    (int) v[5], (int) v[6], v[0], exerciseParentID));
        }
        if (pos[0] != raw.length)
            throw new IllegalArgumentException("Trailing bytes after " + count + " samples");
        return samples;
    }

    /** True if both lists hold the same measurements in the same order (row IDs ignored). */
    public static boolean sameSamples(List<ExerciseData> a, List<ExerciseData> b) {
        if (a.size() != b.size())
            return false;
        long[] fa = new long[FIELDS];
        long[] fb = new long[FIELDS];
        for (int i = 0; i < a.size(); i++) {
            fields(a.get(i), fa);
            fields(b.get(i), fb);
            for (int f = 0; f < FIELDS; f++)
                if (fa[f] != fb[f])
                    return false;
        }
        return true;
    }

    private static void fields(ExerciseData s, long[] out) {
        out[0] = s.getExTimestamp();
        out[1] = s.getExPower();
        out[2] = s.getExSpeed();
        out[3] = s.getExDistance();
        out[4] = s.getExCalories();
        out[5] = s.getExHR();
        out[6] = s.getExRPM();
    }

    private static byte[] inflate(byte[] blob) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(blob);
            ByteArrayOutputStream out = new ByteArrayOutputStream(blob.length * 4 + 64);
            byte[] buf = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary()))
                    throw new IllegalArgumentException("Truncated sample blob");
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Corrupt sample blob", e);
        } finally {
            inflater.end();
        }
    }

    private static long zigzag(long n) {
        return (n << 1) ^ (n >> 63);
    }

    private static long unzigzag(long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    private static void writeVarint(ByteArrayOutputStream out, long n) {
        while ((n & ~0x7FL) != 0) {
            out.write((int) ((n & 0x7F) | 0x80));
            n >>>= 7;
        }
        out.write((int) n);
    }

    private static long readVarint(byte[] in, int[] pos) {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            if (pos[0] >= in.length)
                throw new IllegalArgumentException("Truncated sample data");
            byte b = in[pos[0]++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                return result;
        }
        throw new IllegalArgumentException("Varint too long");
    }
}
