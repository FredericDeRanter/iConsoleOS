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
import java.util.Objects;

import eu.le_tian.iConsoleOS.communication.BluetoothChatService;
import eu.le_tian.iConsoleOS.communication.ChannelService;
import eu.le_tian.iConsoleOS.communication.Constants;
import eu.le_tian.iConsoleOS.DeviceListActivity;
import eu.le_tian.iConsoleOS.communication.IConsole;
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
    private int selectedDuration;
    private int runningIndex = 0; //variable to follow the time index of the profile that is running.
    //private int doneIndex = 0; //variable to follow the time index of the done profile.
    private boolean runningExercise = false;
    private boolean paused = false;
    private long secondsTotalPaused = 0;
    private long startPause = 0;
    private long stopPause = 0;
    private List<Point> timeStampAndLevelProfile = new ArrayList<Point>();
    //private List<Point> timestampAndLevelBusy = new ArrayList<Point>();
    private int extraLevel = 0;
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
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            enableButtons(true);
                            BTState = "Connected";
                            binding.buttonBluetooth.setBackgroundTintList(res.getColorStateList(R.color.colorlistgreenbutton, null));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            enableButtons(false);
                            BTState = "Connecting";
                            binding.buttonBluetooth.setBackgroundTintList(res.getColorStateList(R.color.colorlistyellowbutton, null));
                            //mLevel.setEnabled(false);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            enableButtons(false);
                            binding.buttonBluetooth.setBackgroundTintList(res.getColorStateList(R.color.colorlistredbutton, null));
                            BTState = "Disconnected";
                            break;
                    }
                    break;
                case Constants.MESSAGE_DATA:
                    if (!(msg.obj instanceof IConsole.Data))
                        return;
                    IConsole.Data data = (IConsole.Data) msg.obj;

                    if (paused) {
                        if (startPause == 0) {
                            startPause = data.mTime;
                            Log.d(TAG, "Start pause: " + startPause);
                        } else {
                            stopPause = data.mTime;
                            Log.d(TAG, "pause continued: " + stopPause);
                        }
                    } else {
                        mChannelService.setSpeed(data.mSpeed10 / 10.0);
                        mChannelService.setPower(data.mPower10 / 10);
                        mChannelService.setCadence(data.mRPM);
                        binding.Speed.setText(String.format("% 3.1f", data.mSpeed10 / 10.0));
                        binding.Power.setText(String.format("% 3.1f", data.mPower10 / 10.0));
                        binding.RPM.setText(String.format("%d", data.mRPM));
                        binding.Distance.setText(String.format("% 3.1f", data.mDistance10 / 10.0));
                        binding.Calories.setText(String.format("% 3d", data.mCalories));
                        binding.Heart.setText(String.format("%d", data.mHF));
                        binding.heartBeat2.setDurationBasedOnBPM(data.mHF);
                        long timeInExercise = data.mTime - secondsTotalPaused;
                        SetTimeDisplay(timeInExercise, selectedDuration);
                        ExerciseData exerciseData = new ExerciseData(0, data.mPower10, data.mSpeed10, data.mDistance10, data.mCalories, data.mHF, data.mRPM, timeInExercise, IConsoleViewModel.runningExercise.getExerciseID());
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

    private String convertLongToString(long timeInLong) {
        StringBuilder b = new StringBuilder();
        long day = timeInLong / 60 / 60 / 24;
        if (day > 0)
            b.append(String.format(Locale.US, "%02d:", day));
        long hour = (timeInLong % (60 * 60 * 24)) / 60 / 60;
        if (hour > 0) {
            if (day > 0)
                b.append(String.format(Locale.US, "%02d:", hour));
            else
                b.append(String.format(Locale.US, "%d:", hour));
        }
        long min = (timeInLong % (60 * 60)) / 60;
        long sec = timeInLong % 60;
        b.append(String.format(Locale.US, "%02d:%02d", min, sec));
        return b.toString();
    }


    /**
     * function to show the time during an exercise and to set the level if a profile is running.
     * and to draw the graph
     * return value not used at the moment
     */

    private boolean SetTimeDisplay(long timePassed, int duration) {
        /**
         * timeStampAndLevelProfile has always at least one data point by selecting any profile.
         *
         * */
        //done: adjust 1 to the profile extra level setting for if no profiledata
        int currentLevel = extraLevel + timeStampAndLevelProfile.get(0).y;  //if no profile is running; this is the default value.

        if (duration == 0 && timePassed > 0) {
            //no duration set, so just need to convert time passed
            binding.Time.setText(convertLongToString(timePassed));
            //DONE: add points to IConsoleViewModel.timeStampsAndLevelsDone
            //when selecting any profile, something is added in pointsAll always. That's enough to start drawing the graph (and keep drawing)
            //check previous level in graph, if same as current, do nothing. Else add point to done.

            //set the level in the beginning of the exercise
            if (timePassed < 2) {
                setLevel(currentLevel);
            }

            if (IConsoleViewModel.timeStampsAndLevelsDone.isEmpty()) {
                IConsoleViewModel.timeStampsAndLevelsDone.add(new Point((int) timePassed, currentLevel));
            } else {
                //check if level has changed from previous point
                int lastIndex = IConsoleViewModel.timeStampsAndLevelsDone.size() - 1;
                if (IConsoleViewModel.timeStampsAndLevelsDone.get(lastIndex).y != currentLevel) {
                    //different level add another point
                    IConsoleViewModel.timeStampsAndLevelsDone.add(new Point((int) timePassed, currentLevel));
                } else {
                    //change the last point
                    IConsoleViewModel.timeStampsAndLevelsDone.set(lastIndex, new Point((int) timePassed, currentLevel));
                }
            }

        } else if (duration > 0 && timePassed == 0) {
            //setting duration, but not running yet
            binding.Time.setText(convertLongToString(duration));
            //not running so clear the timeStampsAndLevelsDone
            IConsoleViewModel.timeStampsAndLevelsDone.clear();

        } else if (duration > 0 && timePassed > 0) {
            //running a profile with duration

            long leftTime = duration - timePassed;
            binding.Time.setText(convertLongToString(leftTime));
            //set the level according to the profile
            //Should be done: what if no profile data (only duration)?
            //DONE: create level adjuster variable (set with ++,--, + and - button

            /**
             * options:
             *  - runningIndex is last point in profile: just set currentlevel to runningindex + extralevel.
             *  - runningIndex is within profile and not last point:
             *      - check if the passed time is greater or equal than next runningIndex, if so set new runningIndex, set new level
             *      - else currentlevel is runningIndex + extraLevel
             *
             *      Add safeguard to see if runningLevel is within size of timeStampAndLevelProfile and if not, then just set to last item of timeStampAndLevelProfile (if it's not empty)
             * */
            if (leftTime > 0) {
                if (runningIndex < timeStampAndLevelProfile.size()) {
                    currentLevel = timeStampAndLevelProfile.get(runningIndex).y + extraLevel;

                    //needs changing and setting after this if
                    if ((runningIndex < timeStampAndLevelProfile.size() - 1) && (timePassed >= timeStampAndLevelProfile.get(runningIndex + 1).x)) {
                        runningIndex++; //adjust runningIndex to next level.
                        currentLevel = timeStampAndLevelProfile.get(runningIndex).y + extraLevel;
                        setLevel(currentLevel);
                    }

                    //also set level in the first second of the exercise (else the first level is never set)
                    if (runningIndex == 0 && timePassed < 2) {
                        setLevel(currentLevel);
                    }

                } else if (!timeStampAndLevelProfile.isEmpty()) { //next two should never happen
                    Log.e(TAG, "running index went too far?: " + runningIndex + " | size of timeStampAndLevelProfile: " + timeStampAndLevelProfile.size());
                    runningIndex = timeStampAndLevelProfile.size() - 1;
                    currentLevel = timeStampAndLevelProfile.get(runningIndex).y + extraLevel;
                } else {
                    //no idea what to set it to?
                    Log.e(TAG, "running index: " + runningIndex + " | size of timeStampAndLevelProfile: " + timeStampAndLevelProfile.size());
                    runningIndex = 0;
                    currentLevel = 1;
                }

                Log.d(TAG, "currentLevel " + currentLevel + "| extraLevel: " + extraLevel);
                if (IConsoleViewModel.timeStampsAndLevelsDone.isEmpty()) {
                    IConsoleViewModel.timeStampsAndLevelsDone.add(new Point((int) timePassed, currentLevel));
                } else {
                    //check if level has changed from previous point
                    int lastIndex = IConsoleViewModel.timeStampsAndLevelsDone.size() - 1;
                    if (IConsoleViewModel.timeStampsAndLevelsDone.get(lastIndex).y != currentLevel) {
                        //different level add another point
                        IConsoleViewModel.timeStampsAndLevelsDone.add(new Point((int) timePassed, currentLevel));
                    } else {
                        //change the last point
                        IConsoleViewModel.timeStampsAndLevelsDone.set(lastIndex, new Point((int) timePassed, currentLevel));
                    }
                }
            } else if (leftTime <= 0) {
                //if the exercise is finished running
                //TODO: decide what to show when exercise is done. Maybe a user setting: if exercise is finished, stop or continue?
                //exercise is done, but keeps running
                binding.Time.setText(convertLongToString(timePassed));
                //DONE: keep adjusting graph

                //if continue running, add new points to pointsDone. The graph will adjust itself accordingly. (no need to add points to pointAll.
                //runningIndex-1 is the last index of the profile that was running.
                //first check if profile is not empty.
                if (!timeStampAndLevelProfile.isEmpty()) {
                    currentLevel = timeStampAndLevelProfile.get(runningIndex).y + extraLevel;
                    int lastIndex = IConsoleViewModel.timeStampsAndLevelsDone.size() - 1;
                    if (IConsoleViewModel.timeStampsAndLevelsDone.get(lastIndex).y != currentLevel) {
                        //different level add another point
                        IConsoleViewModel.timeStampsAndLevelsDone.add(new Point((int) timePassed, currentLevel));
                    } else {
                        //change the last point
                        IConsoleViewModel.timeStampsAndLevelsDone.set(lastIndex, new Point((int) timePassed, currentLevel));
                    }
                }
            }
        } else if (duration == 0) {
            //no duration and no running exercise. so just set display to 00:00
            binding.Time.setText(convertLongToString(duration));
            //not running so clear the timeStampsAndLevelsDone
            IConsoleViewModel.timeStampsAndLevelsDone.clear();
        }
        binding.homeGraphView.setPointsDone(IConsoleViewModel.timeStampsAndLevelsDone);
        return false;
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
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mChatService = null;

        }
    };
    private ServiceConnection mChannelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            Log.v(TAG, "mChannelServiceConnection.onServiceConnected...");

            mChannelService = (ChannelService.ChannelServiceComm) serviceBinder;


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
            }
        }
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        res = getResources();
        ArrayAdapter adapter = new ArrayAdapter(getContext(), R.layout.item_exerciseprofile);
        binding.homeProfileSpinner.setAdapter(adapter);
        IConsoleViewModel.getAllExerciseProfiles().observe(getViewLifecycleOwner(), exerciseProfiles -> adapter.addAll(exerciseProfiles));

        IConsoleViewModel.getAllExerciseProfileDataSelected().observe(getViewLifecycleOwner(), exerciseProfileDataList -> {
            /**
             * Here we react on change in profiledata from the selected profile.
             * check if duration set in profile (different than 0), this can be set in the UI and added buttons to adjust.
             * If duration is set to 0 and no profiledata exist. The time textview is set to 00:00 and no buttons.
             * If no duration and there is data: calculate total time from data and set to time textview, add buttons to change the time.
             * start countdown when press start and make time buttons go away? (or count up when no time is set)
             * */
            ExerciseProfile exerciseProfileSelected = IConsoleViewModel.getSelectedExerciseProfile();
            int durationInProfile = exerciseProfileSelected.getDefaultDuration();
            int profileDefaultLevel = exerciseProfileSelected.getStartLevel();

            if (durationInProfile > 0) {
                //DONE: what if no exerciseProfiledata is set?: just set one point to the defaultProfileLevel
                SetTimeDisplay(0, durationInProfile);
                selectedDuration = durationInProfile;
                IConsoleViewModel.CreateTimeStampsAndLevels(exerciseProfileDataList, selectedDuration, exerciseProfileSelected.isRepeatToFill(), profileDefaultLevel);
            } else if ((exerciseProfileDataList != null) && (exerciseProfileDataList.size() > 0)) {
                /** profiledata is not empty, so need to calculate duration*/
                selectedDuration = IConsoleViewModel.getMinDuration(exerciseProfileDataList);
                SetTimeDisplay(0, selectedDuration);
                IConsoleViewModel.CreateTimeStampsAndLevels(exerciseProfileDataList, selectedDuration, exerciseProfileSelected.isRepeatToFill(), profileDefaultLevel);
                Log.d(TAG, "DataList:" + exerciseProfileDataList.size() + "  no duration ");
            } else {
                /** no profiledata and no duration: quick start */
                binding.Time.setText(String.format("%02d:%02d", 0, 0));
                selectedDuration = durationInProfile;
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
        IConsoleViewModel.getTimeStampsAndLevelsProfile().observe(getViewLifecycleOwner(), timeStampAndLevelProfile -> {
            binding.homeGraphView.setPointsAll(timeStampAndLevelProfile);
            this.timeStampAndLevelProfile = timeStampAndLevelProfile;
        });

        binding.homeProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                /**set the selectedProfileID in viewmodel to trigger a transformer switchmap on the livedata of exerciseProfileData */
                ExerciseProfile exerciseProfileSelected = (ExerciseProfile) adapter.getItem(position);
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

        /**
         * Start button actions
         * */
        binding.buttonStart.setOnClickListener(v -> {
            //DONE: make a running boolean or state int (0: stopped, 1: running, 2: paused) mchatservice needs to be not null to be able to communicate.
            if (mChatService != null) {
                if (!runningExercise) {
                    //start running
                    runningExercise = true;
                    mChatService.startIConsole();
                    keepScreenOn(true);
                    //running index to 0 since starting new exercise. need to create way to pauze exercise.
                    runningIndex = 0;
                    //init variables to be able to pause
                    paused = false;
                    secondsTotalPaused = 0;
                    extraLevel = 0;
                    //clear the progress
                    List<Point> tempEmpty = new ArrayList<>();
                    binding.homeGraphView.setPointsDone(tempEmpty);
                    //timestampAndLevelBusy.clear();

                    /**
                     * Start recording an exercise:
                     * */
                    //TODO: change the user ID to the selected user
                    Exercise exercise = new Exercise(0, new Date().getTime(), 0, IConsoleViewModel.selectedExerciseProfile.getExerciseProfileName(), 1);
                    IConsoleViewModel.insertExercise(exercise);

                    /** change to pauze button*/
                    changeStartButton(2);

                    /** disable profile selector **/
                    binding.homeProfileSpinner.setEnabled(false);
                    Log.d(TAG, "exercise started");

                    /** start the heart beat*/
                    binding.heartBeat2.start();

                } else if (paused) {

                    /**
                     * It's paused, now start running again.
                     * calculate total time it was paused
                     * **/
                    long tempPaused = stopPause - startPause;
                    //TODO: check if the +1 is needed
                    secondsTotalPaused += tempPaused + 1;
                    paused = false;
                    startPause = 0;
                    stopPause = 0;
                    changeStartButton(2);

                    Log.d(TAG, "exercise was paused, starting again, second paused: " + tempPaused);

                } else {
                    /** How to pause:
                     * stop recording by checking paused boolean in handler
                     * keep amount of seconds that nothing happens
                     * when starting again, substract the paused time from the actual timing. For the rest; the data can continue.
                     * */
                    paused = true;
                    /**Change to blinking run/pauze button (until implemented back to run button*/
                    //TODO: implement blinking paused/run button
                    changeStartButton(1);
                    Log.d(TAG, "exercise paused: " + paused);
                }
            }

        });


        /**
         * Stop button action
         * */
        binding.buttonStop.setOnClickListener(v -> {
            binding.levelTextView.setText("1");
            binding.levelSeekbar.setValue(1);
            if (mChatService != null) {
                if (runningExercise) {
                    //only stop if running
                    /**Change start button to show as start */
                    changeStartButton(1);
                    mChatService.stopIConsole();
                    keepScreenOn(false);
                    /** enable profile selector **/
                    binding.homeProfileSpinner.setEnabled(true);
                    runningExercise = false;
                    /** stop the heart beat*/
                    binding.heartBeat2.stop();

                }
            }
        });

        /**
         * normal click BT button will open select device if not connected, else show toast to say long press to disconnect
         */
        binding.buttonBluetooth.setOnClickListener(v -> {
            /*
             * Depending on BT state this button should have different actions:
             * When disconnected: list devices to connect to (big red BT button)
             * When connected: disconnect from device (smaller green BT button, maybe need to double click to disonnect? so you can't do by accident)
             * */
            if (BTState == "Connected") {
                Snackbar.make(v, "Keep holding to disconnect Bluetooth", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
/*
                mLevel.setValue(1);
                if (mChatService != null)
                    mChatService.stopBT();
*/
            } else { //show list BT devices
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
            //
        });

        /**
         * Long click BT button to disconnect
         * */
        binding.buttonBluetooth.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //disconnect if connected.
                Snackbar.make(v, "Bluetooth disconnecting", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                if (BTState == "Connected") {
                    if (mChatService != null)
                        mChatService.stopBT();
                }
                //binding.buttonBluetooth.setBackgroundTintList(ColorStateList.valueOf(res.getColor(R.color.colorYellow)));
                return true;
            }
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
            int oldvalue = Integer.parseInt(binding.levelTextView.getText().toString());
            setLevel(oldvalue + 1);
        });

        binding.plusPlusButton.setOnClickListener(v -> {
            int oldvalue = Integer.parseInt(binding.levelTextView.getText().toString());
            setLevel(oldvalue + 5);
        });

        binding.minButton.setOnClickListener(v -> {
            int oldvalue = Integer.parseInt(binding.levelTextView.getText().toString());
            //make sure that the value can't be smaller than 1
            //if (oldvalue<2) {
            //    oldvalue = 2;
            //}
            setLevel(oldvalue - 1);
        });

        binding.minMinButton.setOnClickListener(v -> {
            int oldvalue = Integer.parseInt(binding.levelTextView.getText().toString());
            //make sure that the value can't be smaller than 1
            /*if (oldvalue<6) {
                oldvalue = 6;
            }*/
            setLevel(oldvalue - 5);
        });

        binding.levelSeekbar.setOnBoxedPointsChangeListener(new BoxedVertical.OnValuesChangeListener() {
            @Override
            public void onPointsChanged(BoxedVertical boxedPoints, final int value) {
                //System.out.println(value);
                Log.v(TAG, "BoxedVertical Changed Value to " + value);
                int oldvalue = Integer.parseInt(binding.levelTextView.getText().toString());
                if (value != oldvalue) {
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
        if (on) {
            Objects.requireNonNull(getActivity()).getWindow().addFlags(flags);
        } else {
            getActivity().getWindow().clearFlags(flags);
        }
    }

    /**
     * function to set level and adjust the text and bar
     */
    private void setLevel(int newVal) {
        if (mChatService != null) {
            //see that value is between 1 and 32
            if (newVal > 32) {
                newVal = 32;
            } else if (newVal < 1) {
                newVal = 1;
            }
            if (!mChatService.setLevel(newVal)) {
                Log.e(TAG, "setLevel failed");
            } else {
                binding.levelTextView.setText(String.valueOf(newVal));
                binding.levelSeekbar.setValue(newVal);
                //check the level in the running profile (if any) and set the extraLevel accordingly
                if (!timeStampAndLevelProfile.isEmpty()) { //this should never be empty
                    extraLevel = newVal - timeStampAndLevelProfile.get(runningIndex).y;
                    Log.d(TAG, "extraLevel set to " + extraLevel + "| newVal: " + newVal + "| level according to profile:" + timeStampAndLevelProfile.get(runningIndex).y + "| runningIndex: " + runningIndex);
                    //also set it on the graph:
                    binding.homeGraphView.setFutureLevelChange(extraLevel);
                }
            }
        }
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
        binding.buttonStart.setEnabled(enable);
        binding.buttonStop.setEnabled(enable);
        binding.plusButton.setEnabled(enable);
        binding.plusPlusButton.setEnabled(enable);
        binding.minButton.setEnabled(enable);
        binding.minMinButton.setEnabled(enable);
        binding.levelSeekbar.setEnabled(enable);
        if (enable) {
            binding.levelTextView.setText("1");
            binding.levelSeekbar.setValue(1);
        }
    }


    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        if (mChatService == null)
            return;
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        mChatService.startIConsole();
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
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
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

}