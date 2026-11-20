package eu.le_tian.iConsoleOS.data;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BackupFilesTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

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
    public void safetyCopyNameHasSeconds() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 20, 18, 5, 9);
        assertEquals("IConsoleDB-20261120-180509.db", BackupFiles.safetyCopyName(c.getTime()));
    }

    @Test
    public void pruneKeepsNewestThreeAndIgnoresOtherFiles() throws IOException {
        File dir = tmp.newFolder("pre-restore");
        String[] names = {"IConsoleDB-20261101-100000.db", "IConsoleDB-20261120-090000.db",
                "IConsoleDB-20261105-120000.db", "IConsoleDB-20261119-235959.db", "notes.txt",
                "IConsoleDB-20261120-0900.db" /* user-style name, not a safety copy */};
        for (String n : names)
            assertTrue(new File(dir, n).createNewFile());

        BackupFiles.pruneSafetyCopies(dir, 3);

        Set<String> left = new HashSet<>(Arrays.asList(dir.list()));
        assertEquals(new HashSet<>(Arrays.asList("IConsoleDB-20261120-090000.db", "IConsoleDB-20261105-120000.db",
                "IConsoleDB-20261119-235959.db", "notes.txt", "IConsoleDB-20261120-0900.db")), left);
    }

    @Test
    public void restorableVersions() {
        assertFalse(BackupFiles.isRestorableVersion(4, 8));  // no migration from 4
        assertTrue(BackupFiles.isRestorableVersion(5, 8));
        assertTrue(BackupFiles.isRestorableVersion(8, 8));
        assertFalse(BackupFiles.isRestorableVersion(9, 8));  // newer than this build
    }
}
