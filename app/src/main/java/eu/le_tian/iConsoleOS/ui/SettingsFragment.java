package eu.le_tian.iConsoleOS.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.net.Uri;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.le_tian.iConsoleOS.communication.BtLog;
import eu.le_tian.iConsoleOS.communication.BtSettings;
import eu.le_tian.iConsoleOS.communication.ChannelService;
import eu.le_tian.iConsoleOS.data.BackupFiles;
import eu.le_tian.iConsoleOS.data.DatabaseBackup;
import eu.le_tian.iConsoleOS.data.IConsoleRepository;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;


import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.databinding.FragmentSettingsBinding;


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

    private static final long HR_SEARCH_MS = 60_000;
    private ChannelService.ChannelServiceComm mAnt;
    private boolean mAntBound;
    private long mHrSearchUntil;
    private final Handler mUi = new Handler(Looper.getMainLooper());
    private final Runnable mHrTicker = new Runnable() {
        @Override
        public void run() {
            if (mHrSearchUntil != 0 && SystemClock.elapsedRealtime() > mHrSearchUntil)
                stopHeartRateSearch();
            showHeartRateStatus();
            if (binding != null)
                mUi.postDelayed(this, 1000);
        }
    };
    private final ServiceConnection mAntConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAnt = (ChannelService.ChannelServiceComm) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAnt = null;
        }
    };

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

        binding.btLogBtn.setOnClickListener(v -> showBluetoothLog());
        setupBluetoothSettings();
        setupHeartRate();
    }

    @Override
    public void onDestroyView() {
        stopHeartRateSearch();
        mUi.removeCallbacks(mHrTicker);
        if (mAntBound) {
            requireContext().unbindService(mAntConnection);
            mAntBound = false;
            mAnt = null;
        }
        super.onDestroyView();
        binding = null;
    }

    /**
     * Heart rate from the watch over ANT+. "Search" listens for 60 s, so pairing and the watch's
     * "Broadcast heart rate" can be checked without the bike.
     */
    private void setupHeartRate() {
        Context c = requireContext();
        mAntBound = c.bindService(new Intent(c, ChannelService.class), mAntConnection, Context.BIND_AUTO_CREATE);
        binding.hrReceiveSwitch.setChecked(BtSettings.heartRateReceive(c));
        binding.hrReceiveSwitch.setOnCheckedChangeListener((b, on) -> BtSettings.setHeartRateReceive(c, on));
        binding.hrSearchBtn.setOnClickListener(v -> {
            if (mAnt == null) {
                Snackbar.make(v, "ANT+ is not ready yet, try again in a moment", Snackbar.LENGTH_SHORT).show();
                return;
            }
            mHrSearchUntil = SystemClock.elapsedRealtime() + HR_SEARCH_MS;
            mAnt.setHeartRateSearch(true);
            showHeartRateStatus();
        });
        binding.hrForgetBtn.setOnClickListener(v -> {
            BtSettings.setHeartRateDevice(c, 0);
            if (mAnt != null)
                mAnt.restartHeartRate();
            showHeartRateStatus();
        });
        mUi.post(mHrTicker);
    }

    private void stopHeartRateSearch() {
        if (mHrSearchUntil != 0 && mAnt != null)
            mAnt.setHeartRateSearch(false);
        mHrSearchUntil = 0;
    }

    private void showHeartRateStatus() {
        if (binding == null)
            return;
        int paired = BtSettings.heartRateDevice(requireContext());
        String status;
        if (mAnt != null && mAnt.isHeartRateChannelOpen()) {
            int bpm = mAnt.getHeartRate();
            if (bpm > 0)
                status = "Receiving " + bpm + " bpm from monitor " + mAnt.getHeartRateDevice();
            else
                status = "Searching" + (paired != 0 ? " for monitor " + paired : "")
                        + "... On the watch: heart rate widget → Broadcast Heart Rate.";
            if (mHrSearchUntil != 0)
                status += String.format(Locale.US, " (%d s)",
                        Math.max(0, (mHrSearchUntil - SystemClock.elapsedRealtime()) / 1000));
        } else if (mHrSearchUntil != 0) {
            status = "Can't search: no ANT+ channel available (see the Bluetooth log).";
        } else if (paired != 0) {
            status = "Paired with heart-rate monitor " + paired + ".";
        } else {
            status = "No heart-rate monitor paired yet: tap Search while the watch broadcasts heart rate.";
        }
        binding.hrStatusText.setText(status);
        binding.hrSearchBtn.setEnabled(mHrSearchUntil == 0);
        binding.hrForgetBtn.setEnabled(paired != 0);
    }


    private void setupBluetoothSettings() {
        Context c = requireContext();
        binding.antBroadcastSwitch.setChecked(BtSettings.antBroadcast(c));
        binding.antBroadcastSwitch.setOnCheckedChangeListener((b, on) -> BtSettings.setAntBroadcast(c, on));

        int[] choices = BtSettings.IDLE_CHOICES;
        String[] labels = new String[choices.length];
        int selected = 0;
        int current = BtSettings.idleMinutes(c);
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i] == 0 ? "never" : choices[i] + " minutes";
            if (choices[i] == current) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.idleDisconnectSpinner.setAdapter(adapter);
        binding.idleDisconnectSpinner.setSelection(selected, false);
        binding.idleDisconnectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (choices[position] != BtSettings.idleMinutes(c))
                    BtSettings.setIdleMinutes(c, choices[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /** Last part of the persistent Bluetooth/ANT+ log, newest at the top. */
    private void showBluetoothLog() {
        List<String> lines = BtLog.tail(400);
        Collections.reverse(lines);
        TextView text = new TextView(requireContext());
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        text.setTextIsSelectable(true);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad);
        text.setText(lines.isEmpty() ? "Nothing logged yet." : TextUtils.join("\n", lines));
        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(text);
        new AlertDialog.Builder(requireContext())
                .setTitle("Bluetooth log (newest first)")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
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

    /** Asks which backup to restore, then {@link #checkRestore}. */
    private final ActivityResultLauncher<String[]> openBackupDocument = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) checkRestore(uri);
            });

    void importDatabase() {
        if (IConsoleRepository.isRecording()) {
            Snackbar.make(requireView(), "Stop the workout before restoring", Snackbar.LENGTH_LONG).show();
            return;
        }
        // .db files have no registered MIME type, so allow any file; the content is checked.
        openBackupDocument.launch(new String[]{"*/*"});
    }

    /** Copies and checks the chosen file in the background, then asks for confirmation. */
    private void checkRestore(Uri uri) {
        Context appContext = requireContext().getApplicationContext();
        binding.importDbBtn.setEnabled(false);
        new Thread(() -> {
            DatabaseBackup.RestoreCandidate candidate = null;
            String error = null;
            try {
                candidate = DatabaseBackup.prepareRestore(appContext, uri);
            } catch (IOException | RuntimeException e) {
                Log.e("RESTOREDB", "Backup file rejected", e);
                error = String.valueOf(e.getMessage());
            }
            final DatabaseBackup.RestoreCandidate c = candidate;
            final String err = error;
            runOnUi(() -> {
                binding.importDbBtn.setEnabled(true);
                if (c == null) {
                    showMessage("Can't restore this file", err);
                } else {
                    confirmRestore(appContext, c);
                }
            });
        }).start();
    }

    private void confirmRestore(Context appContext, DatabaseBackup.RestoreCandidate c) {
        StringBuilder msg = new StringBuilder("Replace ALL data in the app with this backup?\n\n")
                .append("BACKUP:\n").append(describe(appContext, c.backup))
                .append("\n\nCURRENT DATA:\n").append(describe(appContext, c.current));
        if (c.backup.workouts < c.current.workouts || c.backup.latestStart < c.current.latestStart) {
            msg.append("\n\n\u26A0 The backup is OLDER than the current data. Workouts recorded after it will be gone from the app.");
        }
        msg.append("\n\nA safety copy of the current data is kept in the app (last 3). The app restarts afterwards.");

        new AlertDialog.Builder(requireContext())
                .setTitle("Restore backup")
                .setMessage(msg.toString())
                .setPositiveButton("Restore", (d, w) -> runRestore(appContext, c))
                .setNegativeButton("Cancel", (d, w) -> c.discard())
                .setOnCancelListener(d -> c.discard())
                .show();
    }

    private void runRestore(Context appContext, DatabaseBackup.RestoreCandidate c) {
        binding.importDbBtn.setEnabled(false);
        binding.exportDbBtn.setEnabled(false);
        Snackbar.make(requireView(), "Restoring...", Snackbar.LENGTH_INDEFINITE).show();
        new Thread(() -> {
            try {
                DatabaseBackup.restore(appContext, c);
                Log.i("RESTOREDB", "Restored, restarting");
                restartApp(appContext);
            } catch (IOException | RuntimeException e) {
                Log.e("RESTOREDB", "Restore failed", e);
                // The database may already be closed: restart either way so nothing uses a closed instance.
                runOnUi(() -> new AlertDialog.Builder(requireContext())
                        .setTitle("Restore FAILED")
                        .setMessage(e.getMessage() + "\n\nThe app will restart.")
                        .setPositiveButton("OK", null)
                        .setOnDismissListener(d -> restartApp(appContext))
                        .show());
            }
        }).start();
    }

    private static void restartApp(Context appContext) {
        Intent launch = appContext.getPackageManager().getLaunchIntentForPackage(appContext.getPackageName());
        if (launch != null && launch.getComponent() != null)
            appContext.startActivity(Intent.makeRestartActivityTask(launch.getComponent()));
        Runtime.getRuntime().exit(0);
    }

    private void runOnUi(Runnable r) {
        if (getActivity() != null)
            getActivity().runOnUiThread(() -> {
                if (binding != null) r.run();
            });
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
