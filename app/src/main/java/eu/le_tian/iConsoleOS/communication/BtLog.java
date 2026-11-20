package eu.le_tian.iConsoleOS.communication;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * Small persistent log of Bluetooth / ANT+ events in the app's private storage. Unlike logcat it
 * survives a restart of the tablet, which is the only way out of a hung Bluetooth stack, so after
 * such a hang we can still see what happened before it.
 *
 * <p>Two files of at most {@link #MAX_BYTES} each ({@code bt-log.txt} and the previous one,
 * {@code bt-log.1.txt}). Read them with Settings → Bluetooth log, or:
 * {@code adb exec-out run-as eu.le_tian.iConsoleOS cat files/bt-log.1.txt files/bt-log.txt}
 */
public final class BtLog {
    private static final String TAG = "BtLog";
    static final long MAX_BYTES = 256 * 1024;
    static final String FILE_NAME = "bt-log.txt";
    static final String OLD_FILE_NAME = "bt-log.1.txt";

    private static volatile BtLog sInstance;

    private final File mFile;
    private final File mOldFile;
    private final long mMaxBytes;
    private final LongSupplier mClock;
    private final SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    BtLog(File dir, long maxBytes, LongSupplier clock) {
        mFile = new File(dir, FILE_NAME);
        mOldFile = new File(dir, OLD_FILE_NAME);
        mMaxBytes = maxBytes;
        mClock = clock;
    }

    /** Safe to call more than once; the first call wins. */
    public static void init(Context context) {
        if (sInstance == null) {
            synchronized (BtLog.class) {
                if (sInstance == null)
                    sInstance = new BtLog(context.getApplicationContext().getFilesDir(), MAX_BYTES, System::currentTimeMillis);
            }
        }
    }

    /** Logs to logcat and, once {@link #init} ran, to the persistent file. Never throws. */
    public static void log(String event) {
        Log.i(TAG, event);
        BtLog log = sInstance;
        if (log != null)
            log.append(event);
    }

    /** The last {@code maxLines} lines, oldest first; empty if there is no log yet. */
    public static List<String> tail(int maxLines) {
        BtLog log = sInstance;
        return log != null ? log.readTail(maxLines) : new ArrayList<>();
    }

    synchronized void append(String event) {
        try {
            if (mFile.length() > mMaxBytes) {
                if (mOldFile.exists() && !mOldFile.delete())
                    Log.w(TAG, "Could not delete " + mOldFile);
                if (!mFile.renameTo(mOldFile))
                    Log.w(TAG, "Could not rotate " + mFile);
            }
            try (Writer w = new FileWriter(mFile, true)) {
                w.write(mFormat.format(new Date(mClock.getAsLong())));
                w.write("  ");
                w.write(event.replace('\n', ' '));
                w.write('\n');
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not write the Bluetooth log", e);
        }
    }

    synchronized List<String> readTail(int maxLines) {
        Deque<String> lines = new ArrayDeque<>(maxLines + 1);
        for (File f : new File[]{mOldFile, mFile}) {
            if (!f.exists())
                continue;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lines.addLast(line);
                    if (lines.size() > maxLines)
                        lines.removeFirst();
                }
            } catch (IOException e) {
                lines.addLast("(could not read " + f.getName() + ": " + e.getMessage() + ")");
            }
        }
        return new ArrayList<>(lines);
    }
}
