package eu.le_tian.iConsoleOS.communication;

import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.dsi.ant.channel.AntChannel;
import com.dsi.ant.channel.AntCommandFailedException;
import com.dsi.ant.channel.IAntChannelEventHandler;
import com.dsi.ant.message.ChannelId;
import com.dsi.ant.message.ChannelType;
import com.dsi.ant.message.HighPrioritySearchTimeout;
import com.dsi.ant.message.LowPrioritySearchTimeout;
import com.dsi.ant.message.fromant.BroadcastDataMessage;
import com.dsi.ant.message.fromant.ChannelEventMessage;
import com.dsi.ant.message.fromant.MessageFromAntType;
import com.dsi.ant.message.ipc.AntMessageParcel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Receives heart rate from an ANT+ heart-rate monitor, e.g. a Garmin watch with "Broadcast Heart
 * Rate" on. The opposite of {@link PowerChannelController}: an ANT+ slave that listens.
 *
 * <p>ANT+ HR profile: device type 0x78, period 8070 (4.06 Hz), RF 57. With device number 0 it
 * pairs with the first monitor it hears and reports that device's number, so the caller can
 * remember it and only listen to that one next time. When the monitor goes away the channel
 * searches again, and reopens after a search timeout, until it is closed.
 */
public class HeartRateChannelController {
    private static final String TAG = HeartRateChannelController.class.getSimpleName();
    static final int DEVICE_TYPE = 0x78;
    private static final int PERIOD = 8070;
    private static final int RF_FREQUENCY = 57;

    /** Callbacks run on a background thread. */
    public interface Listener {
        /** The device number of the monitor we are receiving from (also when it was given). */
        void onPaired(int deviceNumber);
    }

    // ANT calls (requestChannelId, reopen) are blocking IPC: keep them off the ANT callback thread.
    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();

    private final HeartRateMonitor mMonitor;
    private final Listener mListener;
    private final int mDeviceNumber;
    private AntChannel mAntChannel;
    private volatile boolean mOpen;
    private volatile boolean mReceiving;
    private volatile boolean mPairReported;

    /**
     * @param deviceNumber the monitor to listen to, or 0 for the first one found
     */
    public HeartRateChannelController(AntChannel antChannel, int deviceNumber, HeartRateMonitor monitor, Listener listener) {
        mAntChannel = antChannel;
        mDeviceNumber = deviceNumber;
        mMonitor = monitor;
        mListener = listener;
        open();
    }

    private void open() {
        if (mAntChannel == null) {
            Log.w(TAG, "No channel available");
            return;
        }
        try {
            mAntChannel.setChannelEventHandler(new EventHandler());
            mAntChannel.assign(ChannelType.BIDIRECTIONAL_SLAVE);
            mAntChannel.setChannelId(new ChannelId(mDeviceNumber, DEVICE_TYPE, 0));
            mAntChannel.setPeriod(PERIOD);
            mAntChannel.setRfFrequency(RF_FREQUENCY);
            // Search in low priority (doesn't disturb the power/speed channels) for the maximum time.
            mAntChannel.setSearchTimeout(LowPrioritySearchTimeout.MAX, HighPrioritySearchTimeout.DISABLED);
            mAntChannel.open();
            mOpen = true;
            BtLog.log("ANT heart rate: searching" + (mDeviceNumber != 0 ? " for monitor " + mDeviceNumber : " for any monitor"));
        } catch (RemoteException e) {
            BtLog.log("ANT heart rate: open failed (remote): " + e.getMessage());
        } catch (AntCommandFailedException e) {
            BtLog.log("ANT heart rate: open failed: " + e.getFailureReason());
            release();
        }
    }

    public void close() {
        mOpen = false;
        mMonitor.clear();
        release();
    }

    private void release() {
        if (mAntChannel != null) {
            mAntChannel.release(); // also closes and unassigns
            mAntChannel = null;
        }
    }

    private void reopenAfterSearchTimeout() {
        sExecutor.execute(() -> {
            AntChannel channel = mAntChannel;
            if (!mOpen || channel == null)
                return;
            try {
                channel.open();
            } catch (RemoteException | AntCommandFailedException e) {
                Log.w(TAG, "reopen failed", e);
            }
        });
    }

    private void reportDeviceNumber() {
        if (mPairReported)
            return;
        mPairReported = true;
        if (mDeviceNumber != 0) {
            mListener.onPaired(mDeviceNumber);
            return;
        }
        sExecutor.execute(() -> {
            AntChannel channel = mAntChannel;
            if (channel == null)
                return;
            try {
                int number = channel.requestChannelId().getChannelId().getDeviceNumber();
                BtLog.log("ANT heart rate: paired with monitor " + number);
                mListener.onPaired(number);
            } catch (RemoteException | AntCommandFailedException e) {
                mPairReported = false; // try again on the next page
                Log.w(TAG, "requestChannelId failed", e);
            }
        });
    }

    private class EventHandler implements IAntChannelEventHandler {
        @Override
        public void onChannelDeath() {
            mReceiving = false;
            BtLog.log("ANT heart rate channel died");
        }

        @Override
        public void onReceiveMessage(MessageFromAntType type, AntMessageParcel parcel) {
            switch (type) {
                case BROADCAST_DATA: // HR monitors only broadcast
                    byte[] page = new BroadcastDataMessage(parcel).getPayload();
                    if (mMonitor.onPage(page, SystemClock.elapsedRealtime()) && !mReceiving) {
                        mReceiving = true;
                        BtLog.log("ANT heart rate: receiving, " + HeartRateMonitor.computedHeartRate(page) + " bpm");
                    }
                    reportDeviceNumber();
                    break;
                case CHANNEL_EVENT:
                    switch (new ChannelEventMessage(parcel).getEventCode()) {
                        case RX_FAIL_GO_TO_SEARCH:
                            if (mReceiving) {
                                mReceiving = false;
                                BtLog.log("ANT heart rate: signal lost, searching");
                            }
                            break;
                        case CHANNEL_CLOSED: // search timed out
                            mReceiving = false;
                            reopenAfterSearchTimeout();
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
