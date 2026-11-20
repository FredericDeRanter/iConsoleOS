package eu.le_tian.iConsoleOS.communication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import eu.le_tian.iConsoleOS.MainActivity;
import eu.le_tian.iConsoleOS.R;

public class BluetoothChatService extends Service {
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    // Debugging
    private static final String TAG = "BluetoothChatService";
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
    // Unique UUID for this application
    private static final UUID SERIAL_PORT_CLASS = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    // Member fields
    private final BluetoothAdapter mAdapter;
    // This is the object that receives interactions from clients.  See RemoteService for a more complete example.
    private final IBinder mBinder = new BluetoothChatServiceI();
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private volatile int mState; // read by the connection threads
    private int mNewState;
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;

    public BluetoothChatService() {
        super();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        BtLog.init(this);
        BtLog.log("Bluetooth service started, adapter " + (mAdapter == null ? "missing" : adapterStateName(mAdapter.getState())));
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mSystemReceiver, filter);
    }

    /** How long the adapter may stay in TURNING_ON / TURNING_OFF before we call it stuck. */
    private static final long STUCK_AFTER_MS = 15_000;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private int mTransitionState = -1;

    private final Runnable mStuckCheck = this::checkStuck;

    private void checkStuck() {
        if (mAdapter != null && mAdapter.getState() == mTransitionState)
            reportStuck("Bluetooth has been " + adapterStateName(mTransitionState) + " for " + STUCK_AFTER_MS / 1000 + " s");
    }

    private final BroadcastReceiver mSystemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                BtLog.log("adapter " + adapterStateName(prev) + " -> " + adapterStateName(state));
                onAdapterStateChanged(prev, state);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                BtLog.log("screen on");
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                BtLog.log("screen off, connection " + stateName(mState));
            }
        }
    };

    private void onAdapterStateChanged(int prev, int state) {
        mMainHandler.removeCallbacks(mStuckCheck);
        if (state == BluetoothAdapter.STATE_TURNING_ON || state == BluetoothAdapter.STATE_TURNING_OFF) {
            mTransitionState = state;
            mMainHandler.postDelayed(mStuckCheck, STUCK_AFTER_MS);
        }
        if (prev == BluetoothAdapter.STATE_TURNING_ON && state == BluetoothAdapter.STATE_OFF)
            reportStuck("Bluetooth failed to turn on");
    }

    /** Logs a stuck Bluetooth stack and tells the UI, which advises restarting the tablet. */
    void reportStuck(String what) {
        BtLog.log("STUCK: " + what);
        Handler h = mHandler;
        if (h != null)
            h.obtainMessage(Constants.MESSAGE_BT_STUCK, what).sendToTarget();
    }

    static String adapterStateName(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF: return "OFF";
            case BluetoothAdapter.STATE_TURNING_ON: return "TURNING_ON";
            case BluetoothAdapter.STATE_ON: return "ON";
            case BluetoothAdapter.STATE_TURNING_OFF: return "TURNING_OFF";
            default: return "state " + state;
        }
    }

    static String stateName(int state) {
        switch (state) {
            case STATE_NONE: return "none";
            case STATE_LISTEN: return "listen";
            case STATE_CONNECTING: return "connecting";
            case STATE_CONNECTED: return "connected";
            default: return "state " + state;
        }
    }

    public boolean startIConsole() {
        BtLog.log("workout start");
        ConnectedThread t = mConnectedThread;
        return t != null && t.startIConsole();
    }

    public boolean stopIConsole() {
        BtLog.log("workout stop");
        ConnectedThread t = mConnectedThread;
        return t != null && t.stopIConsole();
    }

    public boolean setLevel(int level) {
        ConnectedThread t = mConnectedThread;
        return t != null && t.setLevel(level);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        BtLog.log("connect to " + device.getName() + " (" + device.getAddress() + ")");

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");

        return mBinder;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        Handler h = mHandler;
        if (h != null)
            h.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /** Posts a toast text to the UI if one is attached. */
    private void toast(String text) {
        Handler h = mHandler;
        if (h == null)
            return;
        Message msg = h.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, text);
        msg.setData(bundle);
        h.sendMessage(msg);
    }

    private static void closeQuietly(BluetoothSocket socket) {
        if (socket == null)
            return;
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of socket failed", e);
        }
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically startBT AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received onStartCommand() id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopBT();
        BtLog.log("Bluetooth service stopped");
        mMainHandler.removeCallbacks(mStuckCheck);
        try {
            unregisterReceiver(mSystemReceiver);
        } catch (IllegalArgumentException e) {
            // was not registered
        }

        // Tell the user we stopped.
        Log.i(TAG, "onDestroy");
        super.onDestroy();

    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);
        Intent myIntent = new Intent(this, MainActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, myIntent, PendingIntent.FLAG_IMMUTABLE);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_action_device_access_bluetooth_searching)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    public void startBT() {
        Log.d(TAG, "startBT");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Handler h = mHandler;
        if (h != null) {
            Message msg = h.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            h.sendMessage(msg);
        }
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Stop all threads
     */
    public synchronized void stopBT() {
        stopBT("disconnect requested");
    }

    /** Closes any connection; {@code reason} goes to the Bluetooth log. */
    public synchronized void stopBT(String reason) {
        Log.d(TAG, "stopBT");
        if (mConnectThread != null || mConnectedThread != null)
            BtLog.log("disconnect: " + reason);
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        BtLog.log("connection failed");
        toast("Unable to connect device");

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.startBT();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(String reason) {
        BtLog.log("connection lost: " + reason);
        toast("Device connection was lost");

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.startBT();
    }

    public class BluetoothChatServiceI extends Binder {
        public BluetoothChatService getService() {
            return BluetoothChatService.this;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
            if (mHandler != null) {
                mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mState, -1).sendToTarget();
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private volatile BluetoothSocket mmFallbackSocket;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SERIAL_PORT_CLASS);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            if (mAdapter != null && mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
            }

            // Make a connection to the BluetoothSocket
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Standard socket connect failed, attempting fallback socket...", e);
                BtLog.log("socket connect failed (" + e.getMessage() + "), trying fallback channel 1");
                // Fallback method for Android RFCOMM socket reconnect issue
                try {
                    mmFallbackSocket = (BluetoothSocket) mmDevice.getClass()
                            .getMethod("createRfcommSocket", new Class[]{int.class})
                            .invoke(mmDevice, 1);
                    if (mmFallbackSocket != null) {
                        mmFallbackSocket.connect();
                        synchronized (BluetoothChatService.this) {
                            mConnectThread = null;
                        }
                        BtLog.log("connected via fallback socket");
                        connected(mmFallbackSocket, mmDevice);
                        return;
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "Fallback socket connect failed as well", e2);
                    BtLog.log("fallback socket failed: " + e2);
                    // It was leaked before: an open RFCOMM socket per failed attempt.
                    closeQuietly(mmFallbackSocket);
                    mmFallbackSocket = null;
                }

                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            BtLog.log("connected");
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            closeQuietly(mmSocket);
            closeQuietly(mmFallbackSocket);
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final IConsole mmIConsole;
        private volatile boolean mmCancelled = false;
        private volatile String mmLastError = "connection loop ended";

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;

            mmIConsole = new IConsole(mmInStream, mmOutStream, new IConsole.DataListener() {
                @Override
                public void onData(IConsole.Data data) {
                    //Log.i(TAG, "mConnectedThread: onData");
                    // Share the sent message back to the UI Activity
                    Handler h = mHandler;
                    if (h != null)
                        h.obtainMessage(Constants.MESSAGE_DATA, -1, -1, data).sendToTarget();
                }

                @Override
                public void onError(Exception e) {
                    // processIO() returns false after this; run() then closes the socket and
                    // reports the lost connection, whatever the kind of error.
                    Log.e(TAG, "mConnectedThread Error: ", e);
                    mmLastError = String.valueOf(e.getMessage());
                }
            }, /* new IConsole.DebugListener() {
                @Override
                public void onRead(byte[] buffer) {
                    if (buffer.length > 0) {
                        String hexbuf = IConsole.byteArrayToHex(buffer);

                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(Constants.MESSAGE_READ, hexbuf.length(), -1, hexbuf.getBytes())
                                .sendToTarget();
                    }
                }

                @Override
                public void onWrite(byte[] buffer) {
                    String hexbuf = IConsole.byteArrayToHex(buffer);

                    // Share the sent message back to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, hexbuf.getBytes())
                            .sendToTarget();
                }
            }*/ null);
        }

        public void run() {
            showNotification();

            Log.i(TAG, "BEGIN mConnectedThread");

            while (mState == STATE_CONNECTED) {
                if (!mmIConsole.processIO()) {
                    Log.i(TAG, "processIO = false");
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    ; // ignore
                }
            }
            Log.i(TAG, "END mConnectedThread");
            // Cancel the persistent notification.
            mNM.cancel(NOTIFICATION);
            if (!mmCancelled) {
                // Ended by an error, not by us: before, only an IOException closed the socket
                // and any other error left it open with the state still CONNECTED.
                closeQuietly(mmSocket);
                connectionLost(mmLastError);
            }
        }


        public boolean setLevel(int level) {
            return mmIConsole.setLevel(level);
        }

        public boolean startIConsole() {
            return mmIConsole.start();
        }

        public boolean stopIConsole() {
            return mmIConsole.stop();
        }

        public void cancel() {
            mmCancelled = true;
            mmIConsole.stop();
            closeQuietly(mmSocket);
        }
    }

}
