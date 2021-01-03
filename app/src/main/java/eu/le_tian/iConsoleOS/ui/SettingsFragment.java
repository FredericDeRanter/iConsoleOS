package eu.le_tian.iConsoleOS.ui;

import android.Manifest;
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


    void backupDatabase() {
        int permission = checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            //AppDatabase.getInstance(this).getDatabase().close();
            IConsoleViewModel.emptyWall();
            File db = getContext().getDatabasePath("IConsoleDB");
            //File dbShm = new File(db.getParent(), "my-db-shm");
            //File dbWal = new File(db.getParent(), "my-db-wal");

            File db2 = new File("/sdcard/", "IConsoleDB");
            //File dbShm2 = new File(db2.getParent(), "my-db-shm");
            //File dbWal2 = new File(db2.getParent(), "my-db-wal");

            try {
                copyFile(db.getPath(), "", db2.getPath());
                //FileUtils.copyFile(dbShm, dbShm2);
                //FileUtils.copyFile(dbWal, dbWal2);
            } catch (Exception e) {
                Log.e("SAVEDB", e.toString());
            }
        } else {
            Snackbar.make(getView(), "Please allow access to your storage", Snackbar.LENGTH_LONG)
                    .setAction("Allow", view -> requestPermissions(new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 0)).show();
            //requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);

        }
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