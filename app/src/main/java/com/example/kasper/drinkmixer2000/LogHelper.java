package com.example.kasper.drinkmixer2000;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

/**
 * Created by Kasper on 11-01-2016.
 */
public class LogHelper {
    public final static String TAG ="Pluralsight";

    public final static void logDevice(WifiP2pDevice device){
        Log.i(TAG, "Device Address: " + device.deviceAddress);
        Log.i(TAG, "Device Name: " + device.deviceName);
        Log.i(TAG, "Device Primary Type:  " + device.primaryDeviceType);
        Log.i(TAG, "Device Secondary Type: " + device.secondaryDeviceType);
        Log.i(TAG, "Device Is Group Owner: " + (device.isGroupOwner() ? "Yes" : "No"));
        Log.i(TAG, "Device Status: " + translateDeviceStatus(device.status));
    }

    private static String translateDeviceStatus(int status) {
        return null;
    }
}
