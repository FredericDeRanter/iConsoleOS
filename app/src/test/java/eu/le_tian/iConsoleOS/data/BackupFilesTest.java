package eu.le_tian.iConsoleOS.data;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BackupFilesTest {

    @Test
    public void backupFileNameIsTimestamped() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 20, 18, 5, 59);
        assertEquals("IConsoleDB-20261120-1805.db", BackupFiles.backupFileName(c.getTime()));
    }

    @Test
    public void recognisesSqliteHeader() {
        byte[] ok = "SQLite format 3\0more bytes".getBytes(StandardCharsets.US_ASCII);
        assertTrue(BackupFiles.hasSqliteHeader(ok, ok.length));
        assertFalse(BackupFiles.hasSqliteHeader(ok, 10));                    // short read
        byte[] zip = "PK\3\4 not a database at all".getBytes(StandardCharsets.US_ASCII);
        assertFalse(BackupFiles.hasSqliteHeader(zip, zip.length));
        byte[] almost = "SQLite format 2\0".getBytes(StandardCharsets.US_ASCII);
        assertFalse(BackupFiles.hasSqliteHeader(almost, almost.length));
    }

    @Test
    public void restorableVersions() {
        assertFalse(BackupFiles.isRestorableVersion(4, 8));  // no migration from 4
        assertTrue(BackupFiles.isRestorableVersion(5, 8));
        assertTrue(BackupFiles.isRestorableVersion(8, 8));
        assertFalse(BackupFiles.isRestorableVersion(9, 8));  // newer than this build
    }
}
