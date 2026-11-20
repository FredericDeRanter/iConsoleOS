package eu.le_tian.iConsoleOS.data;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/** Pure helpers for database backup/restore, kept free of Android classes for unit tests. */
public final class BackupFiles {
    /** Oldest DB version we have migrations for. */
    public static final int MIN_RESTORABLE_VERSION = 5;

    private static final byte[] SQLITE_MAGIC = "SQLite format 3\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private BackupFiles() {
    }

    /** e.g. {@code IConsoleDB-20261120-1830.db}, in local time. */
    public static String backupFileName(Date when) {
        return "IConsoleDB-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(when) + ".db";
    }

    /** Name of an automatic safety copy, sortable by time: {@code IConsoleDB-20261120-183005.db}. */
    public static String safetyCopyName(Date when) {
        return "IConsoleDB-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(when) + ".db";
    }

    /**
     * Deletes all but the newest {@code keep} safety copies in {@code dir} (by name, which sorts
     * by time). Other files are left alone.
     */
    public static void pruneSafetyCopies(File dir, int keep) {
        File[] copies = dir.listFiles((d, name) -> name.matches("IConsoleDB-\\d{8}-\\d{6}\\.db"));
        if (copies == null || copies.length <= keep)
            return;
        Arrays.sort(copies, (a, b) -> a.getName().compareTo(b.getName()));
        for (int i = 0; i < copies.length - keep; i++)
            //noinspection ResultOfMethodCallIgnored
            copies[i].delete();
    }

    /** True if the first bytes of a file are the SQLite 3 header. */
    public static boolean hasSqliteHeader(byte[] firstBytes, int length) {
        return length >= SQLITE_MAGIC.length
                && Arrays.equals(Arrays.copyOf(firstBytes, SQLITE_MAGIC.length), SQLITE_MAGIC);
    }

    public static int headerLength() {
        return SQLITE_MAGIC.length;
    }

    /**
     * A backup can be restored if Room can migrate it to {@code currentVersion}. A newer version
     * would make this build crash on open (there's deliberately no destructive fallback).
     */
    public static boolean isRestorableVersion(int version, int currentVersion) {
        return version >= MIN_RESTORABLE_VERSION && version <= currentVersion;
    }
}
