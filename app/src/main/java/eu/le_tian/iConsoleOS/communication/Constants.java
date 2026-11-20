package eu.le_tian.iConsoleOS.communication;


/**
 * Defines several constants used between {@link BluetoothChatService} and the UI.
 */

public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_DATA = 6;
    /** Bluetooth adapter stuck or failed to turn on; obj = description. */
    public static final int MESSAGE_BT_STUCK = 7;
    /** The bike was idle with a workout still running: stop and summarize it. */
    public static final int MESSAGE_AUTO_STOP = 8;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
}
