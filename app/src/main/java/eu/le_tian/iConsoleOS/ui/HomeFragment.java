package eu.le_tian.iConsoleOS.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import abak.tr.com.boxedverticalseekbar.BoxedVertical;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.le_tian.iConsoleOS.communication.BluetoothChatService;
import eu.le_tian.iConsoleOS.communication.BtLog;
import eu.le_tian.iConsoleOS.communication.BtSettings;
import eu.le_tian.iConsoleOS.communication.HeartRateMonitor;
import eu.le_tian.iConsoleOS.util.TimeFormat;
import eu.le_tian.iConsoleOS.workout.ProfileTimeline;
import eu.le_tian.iConsoleOS.workout.WorkoutSession;
import eu.le_tian.iConsoleOS.communication.ChannelService;
import eu.le_tian.iConsoleOS.communication.Constants;
import eu.le_tian.iConsoleOS.DeviceListActivity;
import eu.le_tian.iConsoleOS.communication.IConsole;
import androidx.appcompat.app.AlertDialog;
import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.data.Exercise;
import eu.le_tian.iConsoleOS.data.ExerciseData;
import eu.le_tian.iConsoleOS.data.ExerciseProfile;
import eu.le_tian.iConsoleOS.databinding.FragmentHomeBinding;

/**
 * This fragment is the front page. Here the device can be connected, and an exercise can be started
 *
 *     //TODO: export profiles to json file
 *     //TODO: change the option menu to choose users
 *     //TODO: start button animate from run to pause when pausing.
 *
 *
 */


public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private Resources res;

    private FragmentHomeBinding binding;
    private IConsoleViewModel IConsoleViewModel;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

     /* Array adapter for the conversation thread
     private ArrayAdapter<String> mConversationArrayAdapter;
     */

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /* BT state */
    private String BTState = "Disconnected";

    /* Member object for the chat services */
    private BluetoothChatService mChatService = null;
    private boolean mIsBound;
    private ChannelService.ChannelServiceComm mChannelService;

    /**
     * running profile data
     */
    /**
     * The ride: running/paused, profile position, levels, ridden line (see WorkoutSession). The
     * source of truth; the views only show it (they are recreated when you come back from another
     * screen).
     */
    private final WorkoutSession mSession = new WorkoutSession(
            level -> mChatService != null && mChatService.setLevel(level));
    private long exerciseIDbusy;

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    //@SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    updateConnectionState(msg.arg1);
                    applyAntBroadcast();
                    break;
                case Constants.MESSAGE_DATA:
                    if (!(msg.obj instanceof IConsole.Data))
                        return;
                    IConsole.Data data = (IConsole.Data) msg.obj;

                    // The bike can still answer a READ after Stop was pressed. Without this
                    // guard that sample was stored under the already summarized exercise.
                    if (!mSession.isRunning() || IConsoleViewModel.runningExercise == null)
                        break;

                    WorkoutSession.Tick tick = mSession.onSample(data.mTime);
                    if (tick != null) { // null while paused: nothing recorded
                        if (mChannelService != null) { // null until the ANT+ service is bound
                            mChannelService.setSpeed(data.mSpeed10 / 10.0);
                            mChannelService.setPower(data.mPower10 / 10);
                            mChannelService.setCadence(data.mRPM);
                        }
                        int watchHR = mChannelService != null ? mChannelService.getHeartRate() : 0;
                        int heartRate = HeartRateMonitor.choose(watchHR, data.mHF);
                        if (binding != null) { // no view while another screen is shown; keep recording
                            binding.Speed.setText(String.format("% 3.1f", data.mSpeed10 / 10.0));
                            binding.Power.setText(String.format("% 3.1f", data.mPower10 / 10.0));
                            binding.RPM.setText(String.format("%d", data.mRPM));
                            binding.Distance.setText(String.format("% 3.1f", data.mDistance10 / 10.0));
                            binding.Calories.setText(String.format("% 3d", data.mCalories));
                            binding.Heart.setText(String.format("%d", heartRate));
                            binding.heartBeat2.setDurationBasedOnBPM(heartRate);
                            binding.heartLabel.setText(watchHR > 0 ? "Heart (watch)" : "Heart");
                        }
                        showTime(TimeFormat.clock(tick.displaySeconds));
                        showRideLevel();
                        ExerciseData exerciseData = new ExerciseData(0, data.mPower10, data.mSpeed10, data.mDistance10, data.mCalories, heartRate, data.mRPM, tick.rideSeconds, 0 /* parent ID filled in when stored */);
                        IConsoleViewModel.insertExerciseData(exerciseData);
                    }
                    //binding.Time.setText(String.format("%s", data.getTimeStr()));
                    break;
                case Constants.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_AUTO_STOP:
                    // Bike idle with a workout still running (forgotten): finish it properly.
                    if (mSession.isRunning())
                        stopAndSummarizeExercise();
                    break;
                case Constants.MESSAGE_BT_STUCK:
                    showBluetoothStuck(String.valueOf(msg.obj));
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
    private boolean mChannelServiceBound = false;

    /** Before a ride: the profile's duration (or 0:00) and an empty ridden line. */
    private void showIdleTime() {
        showTime(TimeFormat.clock(mSession.idleDisplaySeconds()));
        if (binding != null)
            binding.homeGraphView.setPointsDone(toPoints(mSession.done()));
    }

    /** The level, the offset from the profile and the ridden line, after a sample or a level change. */
    private void showRideLevel() {
        showLevel(mSession.currentLevel());
        if (binding != null) {
            binding.homeGraphView.setFutureLevelChange(mSession.extraLevel());
            binding.homeGraphView.setPointsDone(toPoints(mSession.done()));
        }
    }

    private static List<Point> toPoints(List<ProfileTimeline.Step> steps) {
        List<Point> points = new ArrayList<>(steps.size());
        for (ProfileTimeline.Step step : steps)
            points.add(new Point(step.second, step.level));
        return points;
    }

    private void showTime(String text) {
        if (binding != null)
            binding.Time.setText(text);
    }

    private void showLevel(int level) {
        if (binding != null) {
            binding.levelTextView.setText(String.valueOf(level));
            binding.levelSeekbar.setValue(level);
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mChatService = ((BluetoothChatService.BluetoothChatServiceI) service).getService();
            ((BluetoothChatService.BluetoothChatServiceI) service).setHandler(mHandler);
            Log.d(TAG, "onServiceConnected()");
            if (mChatService != null) {
                updateConnectionState(mChatService.getState());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mChatService = null;
            updateConnectionState(BluetoothChatService.STATE_NONE);
        }
    };
    private ServiceConnection mChannelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            Log.v(TAG, "mChannelServiceConnection.onServiceConnected...");

            mChannelService = (ChannelService.ChannelServiceComm) serviceBinder;
            applyAntBroadcast();


            Log.v(TAG, "...mChannelServiceConnection.onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "mChannelServiceConnection.onServiceDisconnected...");

            // Clearing and disabling when disconnecting from ChannelService
            mChannelService = null;

            Log.v(TAG, "...mChannelServiceConnection.onServiceDisconnected");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (null == mBluetoothAdapter || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
        if (!mChannelServiceBound) doBindChannelService();

    }

    @Override
    public void onDestroy() {
        if (mChatService != null) {
            mChatService.stopBT();
        }
        Log.d(TAG, "onDestroy()");
        doUnbindService();
        doUnbindChannelService();
        mChannelServiceConnection = null;

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.startBT();
            } else {
                updateConnectionState(mChatService.getState());
            }
        }
        applyAntBroadcast(); // the setting may have changed in Settings
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        IConsoleViewModel = new ViewModelProvider(this).get(IConsoleViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onDestroyView() {
        // The fragment stays alive on the back stack (and keeps recording); only the views go.
        binding = null;
        super.onDestroyView();
    }

    @SuppressLint("DefaultLocale")
    private void updateConnectionState(int state) {
        if (binding == null || res == null) return;
        switch (state) {
            case BluetoothChatService.STATE_CONNECTED:
                setStatus(getString(R.string.title_connected_to, mConnectedDeviceName != null ? mConnectedDeviceName : "Device"));
                enableButtons(true);
                BTState = "Connected";
                binding.buttonBluetooth.setBackgroundTintList(res.getColorStateList(R.color.colorlistgreenbutton, null));
                break;
            case BluetoothChatService.STATE_CONNECTING:
                setStatus(R.string.title_connecting);
                enableButtons(false);
                BTState = "Connecting";
                binding.buttonBluetooth.setBackgroundTintList(res.getColorStateList(R.color.colorlistyellowbutton, null));
                break;
            case BluetoothChatService.STATE_LISTEN:
            case BluetoothChatService.STATE_NONE:
            default:
                setStatus(R.string.title_not_connected);
                enableButtons(false);
                binding.buttonBluetooth.setBackgroundTintList(res.getColorStateList(R.color.colorlistredbutton, null));
                BTState = "Disconnected";
                break;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        res = getResources();
        bindViewListeners();
        if (mChatService != null) {
            updateConnectionState(mChatService.getState());
        }
        ArrayAdapter<ExerciseProfile> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_exerciseprofile);
        binding.homeProfileSpinner.setAdapter(adapter);
        IConsoleViewModel.getAllExerciseProfiles().observe(getViewLifecycleOwner(), exerciseProfiles -> {
            // clear first: every emission used to append the whole list again
            adapter.clear();
            adapter.addAll(exerciseProfiles);
            // keep the selected profile (coming back from another screen used to jump to the first)
            ExerciseProfile selected = IConsoleViewModel.getSelectedExerciseProfile();
            if (selected != null && binding != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).getExerciseProfileID() == selected.getExerciseProfileID()) {
                        binding.homeProfileSpinner.setSelection(i, false);
                        break;
                    }
                }
            }
        });

        IConsoleViewModel.getAllExerciseProfileDataSelected().observe(getViewLifecycleOwner(), exerciseProfileDataList -> {
            /**
             * Here we react on change in profiledata from the selected profile.
             * check if duration set in profile (different than 0), this can be set in the UI and added buttons to adjust.
             * If duration is set to 0 and no profiledata exist. The time textview is set to 00:00 and no buttons.
             * If no duration and there is data: calculate total time from data and set to time textview, add buttons to change the time.
             * start countdown when press start and make time buttons go away? (or count up when no time is set)
             * */
            // The profile can't change during a ride; recomputing would clear the graph.
            if (mSession.isRunning())
                return;
            ExerciseProfile exerciseProfileSelected = IConsoleViewModel.getSelectedExerciseProfile();
            if (exerciseProfileSelected == null)
                return;
            int durationInProfile = exerciseProfileSelected.getDefaultDuration();
            int profileDefaultLevel = exerciseProfileSelected.getStartLevel();

            if (durationInProfile > 0) {
                //DONE: what if no exerciseProfiledata is set?: just set one point to the defaultProfileLevel
                mSession.setDuration(durationInProfile);
                showIdleTime();
                IConsoleViewModel.CreateTimeStampsAndLevels(exerciseProfileDataList, durationInProfile, exerciseProfileSelected.isRepeatToFill(), profileDefaultLevel);
            } else if ((exerciseProfileDataList != null) && (exerciseProfileDataList.size() > 0)) {
                /** profiledata is not empty, so need to calculate duration*/
                int duration = IConsoleViewModel.getMinDuration(exerciseProfileDataList);
                mSession.setDuration(duration);
                showIdleTime();
                IConsoleViewModel.CreateTimeStampsAndLevels(exerciseProfileDataList, duration, exerciseProfileSelected.isRepeatToFill(), profileDefaultLevel);
                Log.d(TAG, "DataList:" + exerciseProfileDataList.size() + "  no duration ");
            } else {
                /** no profiledata and no duration: quick start */
                mSession.setDuration(0);
                showIdleTime();
                //DONE: adjust the CreateTimeStampsAndLevels to add the defaultLevel for the quickstartprofile
                IConsoleViewModel.CreateTimeStampsAndLevels(null, 0, true, profileDefaultLevel);
                Log.d(TAG, "no profiledata and duration:");
            }

            /**
             * When press play button the data profile needs to be converted a double array with time moments when level changes.
             * Also adjusted level is normally 0, but can be adjusted when busy.
             * */

        });

        //observe the profile for drawing the graph
        IConsoleViewModel.getTimeStampsAndLevelsProfile().observe(getViewLifecycleOwner(), timeline -> {
            // Also redelivered to a new view during a ride: draw it; the session keeps its profile.
            binding.homeGraphView.setPointsAll(timeline);
            List<ProfileTimeline.Step> steps = new ArrayList<>(timeline.size());
            for (Point p : timeline)
                steps.add(new ProfileTimeline.Step(p.x, p.y));
            mSession.setProfile(steps);
        });

        binding.homeProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSession.isRunning())
                    return; // the spinner is disabled during a ride; ignore programmatic selections
                /**set the selectedProfileID in viewmodel to trigger a transformer switchmap on the livedata of exerciseProfileData */
                ExerciseProfile exerciseProfileSelected = adapter.getItem(position);
                Log.d(TAG, "selected:" + exerciseProfileSelected.getExerciseProfileID() + " : " + exerciseProfileSelected.getExerciseProfileName());
                /**this updates the livedata in the viewmodel*/
                assert exerciseProfileSelected != null;
                IConsoleViewModel.setSelectedExerciseProfileID(exerciseProfileSelected.getExerciseProfileID());
                IConsoleViewModel.setSelectedExerciseProfile(exerciseProfileSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });

        restoreRideState();
    }

    /** Shows the state of a ride that was running while another screen was shown. */
    private void restoreRideState() {
        showLevel(mSession.currentLevel());
        if (!mSession.isRunning() || binding == null)
            return;
        changeStartButton(mSession.isPaused() ? 1 : 2);
        binding.homeProfileSpinner.setEnabled(false);
        binding.heartBeat2.start();
        showRideLevel();
    }

    void doBindService() {
        Log.d(TAG, "doBindService()");

        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        getActivity().bindService(new Intent(getActivity(), BluetoothChatService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void doBindChannelService() {
        Log.v(TAG, "doBindChannelService...");

        // Binds to ChannelService. ChannelService binds and manages connection between the
        // app and the ANT Radio Service
        mChannelServiceBound = getActivity().bindService(new Intent(getActivity(), ChannelService.class), mChannelServiceConnection, Context.BIND_AUTO_CREATE);

        if (!mChannelServiceBound)   //If the bind returns false, run the unbind method to update the GUI
            doUnbindChannelService();

        Log.i(TAG, "  Channel Service binding = " + mChannelServiceBound);

        Log.v(TAG, "...doBindChannelService");
    }

    private void doUnbindChannelService() {
        Log.v(TAG, "doUnbindChannelService...");

        if (mChannelServiceBound) {
            getActivity().unbindService(mChannelServiceConnection);

            mChannelServiceBound = false;
        }

        Log.v(TAG, "...doUnbindChannelService");
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.v(TAG, "setupChat()");

        if (!mIsBound)
            doBindService();
    }

    /**
     * Wires the buttons of the current view. Called for every new view (onViewCreated): before,
     * this ran only once, from setupChat(), so after another screen Home's buttons did nothing.
     */
    private void bindViewListeners() {
        /**
         * Start button actions
         * */
        binding.buttonStart.setOnClickListener(v -> {
            //DONE: make a running boolean or state int (0: stopped, 1: running, 2: paused) mchatservice needs to be not null to be able to communicate.
            if (mChatService != null) {
                if (!mSession.isRunning()) {
                    if (IConsoleViewModel.selectedExerciseProfile == null) {
                        Snackbar.make(v, "Select a profile first", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    mSession.start();
                    mChatService.startIConsole();
                    keepScreenOn(true);
                    binding.homeGraphView.setPointsDone(new ArrayList<>()); // clear the progress

                    //TODO: change the user ID to the selected user
                    Exercise exercise = new Exercise(0, new Date().getTime(), 0, IConsoleViewModel.selectedExerciseProfile.getExerciseProfileName(), 1);
                    IConsoleViewModel.startExercise(exercise);

                    changeStartButton(2); // pause button
                    binding.homeProfileSpinner.setEnabled(false);
                    binding.heartBeat2.start();
                    Log.d(TAG, "exercise started");
                } else if (mSession.isPaused()) {
                    // Continue: the bike's seconds while paused are left out of the ride time.
                    mSession.resume();
                    changeStartButton(2);
                    Log.d(TAG, "exercise continued");
                } else {
                    // Pause: samples are not recorded until Start is pressed again.
                    mSession.pause();
                    changeStartButton(1);
                    Log.d(TAG, "exercise paused");
                }
            }

        });


        /**
         * Stop button action
         * */
        binding.buttonStop.setOnClickListener(v -> {
            mSession.resetLevel();
            showLevel(1);
            if (mSession.isRunning()) {
                stopAndSummarizeExercise();
            }
        });

        /**
         * normal click BT button will open select device if not connected, else show toast to say long press to disconnect
         */
        binding.buttonBluetooth.setOnClickListener(v -> {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                return;
            }
            if (mChatService == null) {
                setupChat();
            }

            int state = mChatService != null ? mChatService.getState() : BluetoothChatService.STATE_NONE;
            if (state == BluetoothChatService.STATE_CONNECTED) {
                Snackbar.make(v, "Keep holding to disconnect Bluetooth", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else if (state == BluetoothChatService.STATE_CONNECTING) {
                Snackbar.make(v, "Connecting to device...", Snackbar.LENGTH_SHORT).show();
            } else {
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

        binding.buttonBluetooth.setOnLongClickListener(v -> {
            int state = mChatService != null ? mChatService.getState() : BluetoothChatService.STATE_NONE;
            if (state == BluetoothChatService.STATE_CONNECTED || state == BluetoothChatService.STATE_CONNECTING) {
                Snackbar.make(v, "Bluetooth disconnecting...", Snackbar.LENGTH_SHORT).show();
                if (mChatService != null) {
                    mChatService.stopBT();
                }
            } else {
                Snackbar.make(v, "Bluetooth is not connected", Snackbar.LENGTH_SHORT).show();
            }
            return true;
        });

        /**
         * buttons are disabled at start until bluetooth is connected
         * */
        enableButtons(false);

        /**
         * button bluetooth is red since not connected
         * */
        binding.buttonBluetooth.setBackgroundTintList(ColorStateList.valueOf(res.getColor(R.color.colorRed, null)));


        binding.plusButton.setOnClickListener(v -> {
            setLevel(mSession.currentLevel() + 1);
        });

        binding.plusPlusButton.setOnClickListener(v -> {
            setLevel(mSession.currentLevel() + 5);
        });

        binding.minButton.setOnClickListener(v -> {
            setLevel(mSession.currentLevel() - 1);
        });

        binding.minMinButton.setOnClickListener(v -> {
            setLevel(mSession.currentLevel() - 5);
        });

        binding.levelSeekbar.setOnBoxedPointsChangeListener(new BoxedVertical.OnValuesChangeListener() {
            @Override
            public void onPointsChanged(BoxedVertical boxedPoints, final int value) {
                //System.out.println(value);
                Log.v(TAG, "BoxedVertical Changed Value to " + value);
                if (value != mSession.currentLevel()) {
                    setLevel(value);
                }
            }

            @Override
            public void onStartTrackingTouch(BoxedVertical boxedPoints) {
                //Toast.makeText(MainActivity.this, "onStartTrackingTouch", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(BoxedVertical boxedPoints) {
                //Toast.makeText(MainActivity.this, "onStopTrackingTouch", Toast.LENGTH_SHORT).show();
            }

        });
    }

    /**
     * function to change the looking of the start button:
     * type 1: normal start button
     * type 2: pauze button
     * type 3: blinking start/pauze button
     */
    private void changeStartButton(int type) {
        if (binding == null)
            return;
        switch (type) {
            case 1:
                binding.buttonStart.setBackgroundResource(R.drawable.ic_button_start);
                break;
            case 2:
                binding.buttonStart.setBackgroundResource(R.drawable.ic_button_pauze);
                break;
            case 3:
                //Try to make animated pauze/run button
                //AnimatedVectorDrawable startButtonAnim = R.drawable.ic_button_animated_pauze;
                //AnimatedVectorDrawableCompat startButtonAnim = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_button_animated_pauze);
                //binding.buttonStart.setBackgroundResource(startButtonAnim);
                break;
        }
    }


    /**
     * function to keep screen on when exercise started
     */
    private void keepScreenOn(boolean on) {
        //KeyguardManager manager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        int flags = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (getActivity() == null)
            return;
        if (on) {
            getActivity().getWindow().addFlags(flags);
        } else {
            getActivity().getWindow().clearFlags(flags);
        }
    }

    /**
     * function to set level and adjust the text and bar
     */
    private void setLevel(int newVal) {
        if (mSession.requestLevel(newVal))
            showRideLevel();
        else
            Log.e(TAG, "setLevel failed");
    }

    /**
     * Disable rotation when running else BT gets disconnected.
     */
    private void disableRotation(boolean disable) {
        if (getActivity() != null) {
            if (disable)
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            else
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    /**
     * Disable and enable buttons for when connected/disconnected
     */
    private void enableButtons(boolean enable) {

        disableRotation(enable);
        if (binding == null)
            return;
        binding.buttonStart.setEnabled(enable);
        binding.buttonStop.setEnabled(enable);
        binding.plusButton.setEnabled(enable);
        binding.plusPlusButton.setEnabled(enable);
        binding.minButton.setEnabled(enable);
        binding.minMinButton.setEnabled(enable);
        binding.levelSeekbar.setEnabled(enable);
        if (enable) {
            // A fresh connection starts at level 1, but during a ride this is also called on every
            // state refresh (onResume, back from another screen): don't reset the level then.
            if (!mSession.isRunning())
                mSession.resetLevel();
            showLevel(mSession.currentLevel());
        }
    }


    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred. Don't quit: History,
                    // profiles and backups work without Bluetooth.
                    Log.d(TAG, "BT not enabled");
                    BtLog.log("enable request returned without Bluetooth on (result " + resultCode + ")");
                    showBluetoothStuck("Bluetooth was not turned on");
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        if (mChatService != null)
            mChatService.connect(device);
    }

    /**
     * ANT+ (power/speed for the watch) only while the bike is connected and the setting allows it.
     * Before, the channels were open whenever the app ran, also idle for hours.
     */
    private void applyAntBroadcast() {
        Context context = getContext();
        if (mChannelService == null || context == null)
            return;
        boolean connected = mChatService != null && mChatService.getState() == BluetoothChatService.STATE_CONNECTED;
        mChannelService.setBroadcasting(connected && BtSettings.antBroadcast(context));
        mChannelService.setReceivingHeartRate(connected && BtSettings.heartRateReceive(context));
    }

    /**
     * The Bluetooth stack on the tablet sometimes hangs; an app can't reset it, only a restart of
     * the tablet helps. Say so instead of leaving buttons that silently do nothing.
     */
    private void showBluetoothStuck(String what) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Bluetooth problem")
                .setMessage(what + ".\n\nIf Bluetooth can't be switched on in the Android settings either, "
                        + "the Bluetooth system of the tablet is stuck: restart the tablet.\n\n"
                        + "Details are in Settings \u2192 Bluetooth log.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void stopAndSummarizeExercise() {
        if (!mSession.isRunning()) return;

        changeStartButton(1);
        if (mChatService != null) {
            mChatService.stopIConsole();
        }
        keepScreenOn(false);
        if (binding != null) {
            binding.homeProfileSpinner.setEnabled(true);
            binding.heartBeat2.stop();
        }
        mSession.stop();

        java.util.concurrent.Future<Exercise> finishing = IConsoleViewModel != null ? IConsoleViewModel.finishExercise() : null;
        if (finishing != null) {
            new Thread(() -> {
                try {
                    Exercise summary = finishing.get();
                    if (summary != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> showWorkoutSummaryDialog(summary));
                    } else if (getActivity() != null) {
                        getActivity().runOnUiThread(this::resetGraphAndUI);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error summarizing exercise: " + e.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::resetGraphAndUI);
                    }
                }
            }).start();
        } else {
            resetGraphAndUI();
        }
    }

    private void showWorkoutSummaryDialog(Exercise summary) {
        if (getContext() == null) return;

        long durSeconds = summary.getTotalDuration();
        long mins = durSeconds / 60;
        long secs = durSeconds % 60;
        String durationStr = String.format(Locale.US, "%d:%02d", mins, secs);

        double distKm = summary.getTotalDistance() / 10.0;
        double avgSpeedKm = summary.getAvgSpeed() / 10.0;
        double maxSpeedKm = summary.getMaxSpeed() / 10.0;
        double avgPowerW = summary.getAvgPower() / 10.0;
        double maxPowerW = summary.getMaxPower() / 10.0;

        double energyKJ = avgPowerW * durSeconds / 1000.0;

        StringBuilder efforts = new StringBuilder();
        if (summary.getNormPower() > 0)
            efforts.append(String.format(Locale.US, "Normalized Power: %.0f W\n", summary.getNormPower() / 10.0));
        int[] bests = {summary.getBest5min(), summary.getBest20min(), summary.getBest60min()};
        String[] labels = {"5 min", "20 min", "60 min"};
        for (int i = 0; i < bests.length; i++) {
            if (bests[i] > 0)
                efforts.append(String.format(Locale.US, "Best %s: %.0f W\n", labels[i], bests[i] / 10.0));
        }

        String message = String.format(Locale.US,
                "Profile: %s\n\n" +
                "Duration: %s\n" +
                "Distance: %.1f km\n" +
                "Calories: %d kcal  |  Energy: %.0f kJ\n\n" +
                "%s" +
                "Avg Power: %.1f W  |  Max Power: %.1f W\n" +
                "Avg Speed: %.1f km/h  |  Max Speed: %.1f km/h\n" +
                "Avg HR: %d bpm  |  Max HR: %d bpm\n" +
                "Avg Cadence: %d RPM  |  Max Cadence: %d RPM",
                summary.getExProfileName(),
                durationStr,
                distKm,
                summary.getTotalCalories(), energyKJ,
                efforts.length() > 0 ? efforts + "\n" : "",
                avgPowerW, maxPowerW,
                avgSpeedKm, maxSpeedKm,
                summary.getAvgHR(), summary.getMaxHR(),
                summary.getAvgRPM(), summary.getMaxRPM()
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Workout Complete!")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setOnDismissListener(dialog -> resetGraphAndUI())
                .show();
    }

    private void resetGraphAndUI() {
        mSession.reset();
        if (getActivity() == null || binding == null) return; // state reset; no view to update

        showLevel(1);

        binding.Speed.setText(String.format(Locale.US, "% 3.1f", 0.0));
        binding.Power.setText(String.format(Locale.US, "% 3.1f", 0.0));
        binding.RPM.setText(String.format(Locale.US, "%d", 0));
        binding.Distance.setText(String.format(Locale.US, "% 3.1f", 0.0));
        binding.Calories.setText(String.format(Locale.US, "% 3d", 0));
        binding.Heart.setText(String.format(Locale.US, "%d", 0));
        binding.heartBeat2.stop();

        showIdleTime();
        binding.homeGraphView.resetToStandardSettings();
    }

}