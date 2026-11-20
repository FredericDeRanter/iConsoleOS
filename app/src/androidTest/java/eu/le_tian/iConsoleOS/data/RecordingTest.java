package eu.le_tian.iConsoleOS.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Recording a ride through the repository's single recording thread, on an in-memory database
 * (never the app's IConsoleDB). Run on the emulator only, see AGENTS.md.
 */
@RunWith(AndroidJUnit4.class)
public class RecordingTest {
    private IConsoleDatabase db;
    private IConsoleRepository repo;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = Room.inMemoryDatabaseBuilder(ctx, IConsoleDatabase.class).build();
        repo = new IConsoleRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void samplesAreStoredInOrderAndSummarizedAtStop() throws Exception {
        Exercise exercise = new Exercise(0, 1_795_200_000_000L, 0, "Quick Start", 1);
        IConsoleRepository.Recording rec = repo.startRecording(exercise);
        assertTrue("recording flag is set immediately", IConsoleRepository.isRecording());

        // Samples arrive right away, before the exercise insert is guaranteed to be done.
        int n = 500;
        for (int i = 0; i < n; i++)
            repo.recordSample(rec, new ExerciseData(0, 1000 + i, 250, i / 10, i / 20, 120, 70, i / 2, 0));

        Exercise summary = repo.finishRecording(rec).get(10, TimeUnit.SECONDS);
        assertFalse(IConsoleRepository.isRecording());
        assertNotNull(summary);
        assertTrue(summary.isSummarized());
        assertEquals((n - 1) / 2, summary.getTotalDuration());   // last sample's timestamp
        assertEquals(1000 + (n - 1) / 2, summary.getAvgPower());  // average of 1000..1499 (integer)
        assertEquals(1000 + n - 1, summary.getMaxPower());

        // Everything was compacted: no loose rows, all samples in the blob, in recording order.
        List<ExerciseData> stored = repo.getExerciseDataNotLive(summary.getExerciseID()).get(10, TimeUnit.SECONDS);
        assertEquals(n, stored.size());
        for (int i = 0; i < n; i++)
            assertEquals("sample " + i, 1000 + i, stored.get(i).getExPower());
        assertEquals(0, db.exerciseDataDao().getExerciseIDsWithRows().size());
    }

    @Test
    public void finishWithoutSamplesStillSummarizes() throws Exception {
        IConsoleRepository.Recording rec = repo.startRecording(new Exercise(0, 1_795_200_000_000L, 0, "Quick Start", 1));
        Exercise summary = repo.finishRecording(rec).get(10, TimeUnit.SECONDS);
        assertNotNull(summary);
        assertTrue(summary.isSummarized());
        assertEquals(0, summary.getTotalDuration());
    }
}
