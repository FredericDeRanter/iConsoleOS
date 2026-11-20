package eu.le_tian.iConsoleOS.data;

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
