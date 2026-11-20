package eu.le_tian.iConsoleOS.ui;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.text.format.Formatter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import eu.le_tian.iConsoleOS.data.BackupFiles;
import eu.le_tian.iConsoleOS.data.DatabaseBackup;
import eu.le_tian.iConsoleOS.data.IConsoleRepository;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.databinding.FragmentSettingsBinding;

import static androidx.core.content.ContextCompat.checkSelfPermission;

/**
 * This fragment is where to adjust settings and users.
 *
 *      //TODO: add/delete/rename users
 *      //TODO: add defaultprofile selector
 *      //TODO: add plusplus button quick increase amount
 *
 */

public class SettingsFragment extends Fragment {

    private IConsoleViewModel IConsoleViewModel;
    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        IConsoleViewModel = new ViewModelProvider(this).get(IConsoleViewModel.class);
        View root = binding.getRoot();

        return root;


    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        binding.exportDbBtn.setOnClickListener(v -> {
            backupDatabase();
        });

        binding.importDbBtn.setOnClickListener(v -> {
            importDatabase();
        });
    }


    /** Asks where to save, then {@link #runBackup}. */
    private final ActivityResultLauncher<String> createBackupDocument = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> {
                if (uri != null) runBackup(uri);
            });

    void backupDatabase() {
        if (IConsoleRepository.isRecording()) {
            Snackbar.make(requireView(), "Stop the workout before making a backup", Snackbar.LENGTH_LONG).show();
            return;
        }
        createBackupDocument.launch(BackupFiles.backupFileName(new Date()));
    }

    private void runBackup(Uri uri) {
        Context appContext = requireContext().getApplicationContext();
        binding.exportDbBtn.setEnabled(false);
        Snackbar.make(requireView(), "Making backup...", Snackbar.LENGTH_SHORT).show();
        new Thread(() -> {
            String title;
            String message;
            try {
                DatabaseBackup.BackupResult r = DatabaseBackup.backupTo(appContext, uri);
                title = "Backup saved";
                message = describe(appContext, r.info) + (r.verified ? "\n\nChecked: the saved file matches the database."
                        : "\n\nSaved, but this location doesn't allow reading the file back to check it.");
            } catch (IOException | RuntimeException e) {
                Log.e("SAVEDB", "Backup failed", e);
                title = "Backup FAILED";
                message = String.valueOf(e.getMessage());
            }
            final String t = title, m = message;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.exportDbBtn.setEnabled(true);
                    new AlertDialog.Builder(requireContext())
                            .setTitle(t)
                            .setMessage(m)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        }).start();
    }

    static String describe(Context context, DatabaseBackup.Info info) {
        String latest = info.latestStart > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(info.latestStart))
                : "none";
        return String.format(Locale.getDefault(), "%,d workouts, latest %s\n%s, database version %d",
                info.workouts, latest, Formatter.formatShortFileSize(context, info.bytes), info.version);
    }

    void importDatabase() {
        int permission = checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            IConsoleViewModel.emptyWall();
            File db2 = getContext().getDatabasePath("IConsoleDB");
            File db = new File("/sdcard/", "IConsoleDB");
            //File dbShm = new File(db.getParent(), "my-db-shm");
            //File dbWal = new File(db.getParent(), "my-db-wal");
            //File dbShm2 = new File(db2.getParent(), "my-db-shm");
            //File dbWal2 = new File(db2.getParent(), "my-db-wal");

            try {
                Log.d("RESTOREDB", "db restoring");
                copyFile(db.getPath(), "", db2.getPath());
                Log.d("RESTOREDB", "db restored");
            } catch (Exception e) {
                Log.e("RESTOREDB", e.toString());
            }
        } else {
            Snackbar.make(getView(), "Please allow access to your storage", Snackbar.LENGTH_LONG)
                    .setAction("Allow", view -> requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, 0)).show();
        }
    }

    private void copyFile(String inputPath, String inputFile, String outputPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            //check if inputFile is empty or not (if it's empty the path are full paths
            if (inputFile != "") {
                //create output directory if it doesn't exist
                File dir = new File(outputPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                in = new FileInputStream(inputPath + inputFile);
                out = new FileOutputStream(outputPath + inputFile);
            } else {
                in = new FileInputStream(inputPath);
                out = new FileOutputStream(outputPath);
            }

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;

        } catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }


}