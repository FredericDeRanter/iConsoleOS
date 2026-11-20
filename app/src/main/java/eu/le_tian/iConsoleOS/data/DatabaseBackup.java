package eu.le_tian.iConsoleOS.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Backup of the Room database to a user-chosen document (Storage Access Framework).
 * All methods block: call them off the main thread.
 *
 * <p>Android 8 has no {@code VACUUM INTO}, so a backup is: checkpoint the WAL into the main file
 * (TRUNCATE, so the WAL ends up empty), copy the main file, and check the WAL is still empty
 * afterwards. Only checkpoints write the main file and they need WAL content, so an empty WAL
 * before and after the copy proves nothing changed during it. Otherwise it retries.
 */
public final class DatabaseBackup {
    private static final String TAG = "DatabaseBackup";
    public static final String DB_NAME = "IConsoleDB";

    /** What a checked database file contains. */
    public static final class Info {
        public final int version;
        public final int workouts;
        public final long latestStart; // ms, 0 if no workouts
        public final long bytes;

        Info(int version, int workouts, long latestStart, long bytes) {
            this.version = version;
            this.workouts = workouts;
            this.latestStart = latestStart;
            this.bytes = bytes;
        }
    }

    public static final class BackupResult {
        public final Info info;
        /** False if the written document could not be read back to compare (some providers). */
        public final boolean verified;

        BackupResult(Info info, boolean verified) {
            this.info = info;
            this.verified = verified;
        }
    }

    private DatabaseBackup() {
    }

    public static BackupResult backupTo(Context context, Uri target) throws IOException {
        if (IConsoleRepository.isRecording())
            throw new IOException("A workout is being recorded. Stop it before making a backup.");

        File tmp = new File(context.getCacheDir(), "backup-tmp.db");
        try {
            snapshot(context, tmp);
            Info info = inspect(tmp);
            byte[] hash = sha256(tmp);

            ContentResolver resolver = context.getContentResolver();
            try (InputStream in = new FileInputStream(tmp);
                 OutputStream out = resolver.openOutputStream(target, "w")) {
                if (out == null)
                    throw new IOException("Can't write to the chosen file");
                copy(in, out);
            }

            byte[] written = readBackHash(resolver, target);
            if (written != null && !Arrays.equals(hash, written))
                throw new IOException("The saved backup differs from the database. Try again or pick another location.");
            boolean verified = written != null;
            Log.i(TAG, "Backup done: " + info.workouts + " workouts, " + info.bytes + " bytes, verified=" + verified);
            return new BackupResult(info, verified);
        } finally {
            deleteWithSidecars(tmp);
        }
    }

    /** @return SHA-256 of the document, or null if this provider doesn't let us read it back */
    private static byte[] readBackHash(ContentResolver resolver, Uri uri) {
        try (InputStream back = resolver.openInputStream(uri)) {
            return back != null ? sha256(back) : null;
        } catch (SecurityException | IOException e) {
            Log.w(TAG, "Could not read the backup back to verify it", e);
            return null;
        }
    }

    /** Consistent copy of the live database file to {@code out}. */
    static void snapshot(Context context, File out) throws IOException {
        IConsoleDatabase db = IConsoleDatabase.getDatabase(context);
        SupportSQLiteDatabase sql = db.getOpenHelper().getWritableDatabase();
        File dbFile = context.getDatabasePath(DB_NAME);
        File wal = new File(dbFile.getPath() + "-wal");

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (checkpointTruncate(sql) && isEmpty(wal)) {
                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream o = new FileOutputStream(out)) {
                    copy(in, o);
                }
                if (isEmpty(wal))
                    return;
            }
            Log.w(TAG, "Database busy or changed during snapshot, attempt " + attempt);
            try {
                Thread.sleep(300L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
        }
        throw new IOException("The database kept changing during the backup. Try again.");
    }

    /**
     * Opens a copy of a database file and checks it: SQLite header, integrity_check, and reads
     * version and workout count. May switch the copy's journal mode, so only pass copies.
     */
    static Info inspect(File copy) throws IOException {
        byte[] header = new byte[BackupFiles.headerLength()];
        int n;
        try (InputStream in = new FileInputStream(copy)) {
            n = in.read(header);
        }
        if (!BackupFiles.hasSqliteHeader(header, n))
            throw new IOException("Not a database file");

        SQLiteDatabase check;
        try {
            check = SQLiteDatabase.openDatabase(copy.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        } catch (RuntimeException e) {
            throw new IOException("Can't open the database file: " + e.getMessage(), e);
        }
        try {
            try (Cursor c = check.rawQuery("PRAGMA integrity_check", null)) {
                String result = c.moveToFirst() ? c.getString(0) : "no result";
                if (!"ok".equals(result))
                    throw new IOException("Database file is damaged: " + result);
            }
            try (Cursor c = check.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='Exercise'", null)) {
                if (!c.moveToFirst())
                    throw new IOException("Not an iConsoleOS database (no Exercise table)");
            }
            int workouts;
            long latest;
            try (Cursor c = check.rawQuery("SELECT COUNT(*), COALESCE(MAX(exStartDateTime), 0) FROM Exercise", null)) {
                c.moveToFirst();
                workouts = c.getInt(0);
                latest = c.getLong(1);
            }
            return new Info(check.getVersion(), workouts, latest, copy.length());
        } catch (RuntimeException e) {
            throw new IOException("Can't read the database file: " + e.getMessage(), e);
        } finally {
            check.close();
        }
    }

    /** @return true if the checkpoint wasn't blocked by readers or writers */
    private static boolean checkpointTruncate(SupportSQLiteDatabase sql) {
        try (Cursor c = sql.query("PRAGMA wal_checkpoint(TRUNCATE)")) {
            return c.moveToFirst() && c.getInt(0) == 0; // column 0 = busy
        }
    }

    private static boolean isEmpty(File f) {
        return !f.exists() || f.length() == 0;
    }

    static void deleteWithSidecars(File f) {
        for (String suffix : new String[]{"", "-wal", "-shm", "-journal"}) {
            File s = new File(f.getPath() + suffix);
            if (s.exists() && !s.delete())
                Log.w(TAG, "Could not delete " + s);
        }
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) != -1)
            out.write(buf, 0, n);
        out.flush();
    }

    static byte[] sha256(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return sha256(in);
        }
    }

    static byte[] sha256(InputStream in) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1)
                md.update(buf, 0, n);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
