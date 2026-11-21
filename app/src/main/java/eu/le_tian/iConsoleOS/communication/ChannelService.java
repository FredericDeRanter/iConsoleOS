package eu.le_tian.iConsoleOS.communication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.dsi.ant.AntService;
import com.dsi.ant.channel.AntChannel;
import com.dsi.ant.channel.AntChannelProvider;
import com.dsi.ant.channel.ChannelNotAvailableException;
import com.dsi.ant.channel.PredefinedNetwork;

import eu.le_tian.iConsoleOS.BuildConfig;

public class ChannelService extends Service {
    private static final String TAG = "ChannelService";

    private boolean mAntRadioServiceBound;
    private AntService mAntRadioService = null;
    private AntChannelProvider mAntChannelProvider = null;
    private boolean mAllowAddChannel = false;
    /** Only broadcast while the bike is connected (and the setting allows it), not all the time. */
    private boolean mBroadcastWanted = false;
    PowerChannelController powerChannelController = null;
    SpeedChannelController speedChannelController = null;

    /** Receive heart rate while the bike is connected (Home), or while searching from Settings. */
    private boolean mHeartRateForRide = false;
    private boolean mHeartRateSearch = false;
    private HeartRateChannelController heartRateChannelController = null;
    private final HeartRateMonitor mHeartRate = new HeartRateMonitor();
    /** Device number of the monitor we receive from; 0 until known. */
    private volatile int mHeartRateDevice = 0;

    private ServiceConnection mAntRadioServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Must pass in the received IBinder object to correctly construct an AntService object
            mAntRadioService = new AntService(service);

            try {
                // Getting a channel provider in order to acquire channels
                mAntChannelProvider = mAntRadioService.getChannelProvider();

                // Initial check for number of channels available
                boolean mChannelAvailable = mAntChannelProvider.getNumChannelsAvailable() > 0;
                // Initial check for if legacy interface is in use. If the
                // legacy interface is in use, applications can free the ANT
                // radio by attempting to acquire a channel.
                boolean legacyInterfaceInUse = mAntChannelProvider.isLegacyInterfaceInUse();

                // If there are channels OR legacy interface in use, allow adding channels
                BtLog.log("ANT radio service connected, channels available: " + mAntChannelProvider.getNumChannelsAvailable());
                if (mChannelAvailable || legacyInterfaceInUse) {
                    mAllowAddChannel = true;
                } else {
                    // If no channels available AND legacy interface is not in use, disallow adding channels
                    mAllowAddChannel = false;
                }
                openIfWanted();


            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            BtLog.log("ANT radio service disconnected");
            die("Binder Died");

            mAntChannelProvider = null;
            mAntRadioService = null;

            mAllowAddChannel = false;
        }

    };

    /**
     * The interface used to communicate with the ChannelService
     */
    public class ChannelServiceComm extends Binder {

        public void setSpeed(double speed) {
            if (null != speedChannelController) {
                speedChannelController.speed = speed;
            }
        }

        public void setPower(int power) {
            if (null != powerChannelController) {
                powerChannelController.power = power;
            }
        }

        public void setCadence(int cadence) {
            if (null != powerChannelController) {
                powerChannelController.cadence = cadence;
            }
        }

        /** Open the ANT+ power/speed channels when {@code on} and the radio allows it, close them otherwise. */
        public void setBroadcasting(boolean on) {
            ChannelService.this.setBroadcasting(on);
        }

        /** Receive ANT+ heart rate during a ride (bike connected and the setting on). */
        public void setReceivingHeartRate(boolean on) {
            mHeartRateForRide = on;
            updateHeartRateChannel();
        }

        /** Receive ANT+ heart rate for the Settings search, independent of the bike. */
        public void setHeartRateSearch(boolean on) {
            mHeartRateSearch = on;
            updateHeartRateChannel();
        }

        /** After the paired monitor changed in Settings: listen again with the new setting. */
        public void restartHeartRate() {
            closeHeartRateChannel();
            updateHeartRateChannel();
        }

        /** Heart rate from the ANT+ monitor in bpm, or 0 if none was received in the last 5 s. */
        public int getHeartRate() {
            return mHeartRate.heartRate(SystemClock.elapsedRealtime());
        }

        /** Device number of the monitor being received, 0 if not known (yet). */
        public int getHeartRateDevice() {
            return mHeartRateDevice;
        }

        /** Whether the heart-rate channel is open (searching or receiving). */
        public boolean isHeartRateChannelOpen() {
            return heartRateChannelController != null;
        }

        void clearAllChannels() {
            closeAllChannels();
        }
    }

    private void setBroadcasting(boolean on) {
        if (on == mBroadcastWanted)
            return;
        mBroadcastWanted = on;
        BtLog.log("ANT+ broadcast " + (on ? "wanted" : "not wanted"));
        if (on)
            openIfWanted();
        else
            closeBroadcastChannels();
    }

    private synchronized void updateHeartRateChannel() {
        if (mHeartRateForRide || mHeartRateSearch)
            openIfWanted();
        else
            closeHeartRateChannel();
    }

    private synchronized void openIfWanted() {
        if (!mAllowAddChannel)
            return;
        if (mBroadcastWanted && powerChannelController == null) {
            try {
                openAllChannels();
            } catch (ChannelNotAvailableException e) {
                BtLog.log("ANT channels not available: " + e.getMessage());
            }
        }
        if ((mHeartRateForRide || mHeartRateSearch) && heartRateChannelController == null) {
            try {
                int device = BtSettings.heartRateDevice(this);
                mHeartRateDevice = device;
                heartRateChannelController = new HeartRateChannelController(acquireChannel(), device, mHeartRate,
                        number -> {
                            mHeartRateDevice = number;
                            if (BtSettings.heartRateDevice(this) == 0)
                                BtSettings.setHeartRateDevice(this, number);
                        });
            } catch (ChannelNotAvailableException e) {
                BtLog.log("ANT heart-rate channel not available: " + e.getMessage());
            }
        }
    }

    public void openAllChannels() throws ChannelNotAvailableException {
        BtLog.log("ANT channels open (power + speed)");
        powerChannelController = new PowerChannelController(acquireChannel());
        speedChannelController = new SpeedChannelController(acquireChannel());
    }

    private synchronized void closeAllChannels() {
        closeBroadcastChannels();
        closeHeartRateChannel();
    }

    private synchronized void closeHeartRateChannel() {
        if (heartRateChannelController != null) {
            BtLog.log("ANT heart rate: close");
            heartRateChannelController.close();
            heartRateChannelController = null;
        }
        mHeartRateDevice = 0;
    }

    private synchronized void closeBroadcastChannels() {
        if (powerChannelController != null || speedChannelController != null)
            BtLog.log("ANT channels close");
        if (powerChannelController != null) {
            powerChannelController.close();
        }
        if (speedChannelController != null) {
            speedChannelController.close();
        }
        powerChannelController = null;
        speedChannelController = null;
    }

    AntChannel acquireChannel() throws ChannelNotAvailableException {
        AntChannel mAntChannel = null;
        if (null != mAntChannelProvider) {
            try {
                /*
                 * If applications require a channel with specific capabilities
                 * (event buffering, background scanning etc.), a Capabilities
                 * object should be created and then the specific capabilities
                 * required set to true. Applications can specify both required
                 * and desired Capabilities with both being passed in
                 * acquireChannel(context, PredefinedNetwork,
                 * requiredCapabilities, desiredCapabilities).
                 */
                mAntChannel = mAntChannelProvider.acquireChannel(this, PredefinedNetwork.ANT_PLUS_1);
                /*
                NetworkKey mNK = new NetworkKey(new byte[] { (byte)0xb9, (byte)0xa5, (byte)0x21, (byte)0xfb,
                                                             (byte)0xbd, (byte)0x72, (byte)0xc3, (byte)0x45 });
                Log.v(TAG, mNK.toString());
                mAntChannel = mAntChannelProvider.acquireChannelOnPrivateNetwork(this, mNK);
                */
            } catch (RemoteException e) {
                die("ACP Remote Ex");
            }
        }
        return mAntChannel;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return new ChannelServiceComm();
    }

    /**
     * Receives AntChannelProvider state changes being sent from ANT Radio Service
     */
    private final BroadcastReceiver mChannelProviderStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AntChannelProvider.ACTION_CHANNEL_PROVIDER_STATE_CHANGED.equals(intent.getAction())) {
                boolean update = false;
                // Retrieving the data contained in the intent
                int numChannels = intent.getIntExtra(AntChannelProvider.NUM_CHANNELS_AVAILABLE, 0);
                boolean legacyInterfaceInUse = intent.getBooleanExtra(AntChannelProvider.LEGACY_INTERFACE_IN_USE, false);

                if (mAllowAddChannel) {
                    // Was a acquire channel allowed
                    // If no channels available AND legacy interface is not in use, disallow acquiring of channels
                    if (0 == numChannels && !legacyInterfaceInUse) {
                        mAllowAddChannel = false;
                        update = true;
                        closeAllChannels();
                    }
                } else {
                    // Acquire channels not allowed
                    // If there are channels OR legacy interface in use, allow acquiring of channels
                    if (numChannels > 0 || legacyInterfaceInUse) {
                        mAllowAddChannel = true;
                        update = true;
                        openIfWanted();
                    }
                }
            }
        }
    };

    private void doBindAntRadioService() {
        if (BuildConfig.DEBUG) Log.v(TAG, "doBindAntRadioService");

        // Start listing for channel available intents
        registerReceiver(mChannelProviderStateChangedReceiver, new IntentFilter(AntChannelProvider.ACTION_CHANNEL_PROVIDER_STATE_CHANGED));

        // Creating the intent and calling context.bindService() is handled by
        // the static bindService() method in AntService
        mAntRadioServiceBound = AntService.bindService(this, mAntRadioServiceConnection);
    }

    private void doUnbindAntRadioService() {
        if (BuildConfig.DEBUG) Log.v(TAG, "doUnbindAntRadioService");

        // Stop listing for channel available intents
        try {
            unregisterReceiver(mChannelProviderStateChangedReceiver);
        } catch (IllegalArgumentException exception) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Attempting to unregister a never registered Channel Provider State Changed receiver.");
        }

        if (mAntRadioServiceBound) {
            try {
                unbindService(mAntRadioServiceConnection);
            } catch (IllegalArgumentException e) {
                // Not bound, that's what we want anyway
            }

            mAntRadioServiceBound = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAntRadioServiceBound = false;
        BtLog.init(this);
        BtLog.log("ANT service started");
        doBindAntRadioService();

    }

    @Override
    public void onDestroy() {
        closeAllChannels();

        doUnbindAntRadioService();
        mAntChannelProvider = null;

        super.onDestroy();
    }

    static void die(String error) {
        Log.e(TAG, "DIE: " + error);
        BtLog.log("ANT error: " + error);
    }


}
