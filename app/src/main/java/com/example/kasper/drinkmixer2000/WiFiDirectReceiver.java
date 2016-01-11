package com.example.kasper.drinkmixer2000;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by Kasper on 11-01-2016.
 */

public class WiFiDirectReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener{


    private WifiP2pManager _wfdManager;
    private WifiP2pManager.Channel _wfdChannel;
    private MainActivity _appMainActivity;

    private boolean _isWifiDirectEnabled;
    WifiP2pDevice[] _wfdDevices;
    private IntentFilter _intentFilter = null;
    public WiFiDirectReceiver(){}

    public WiFiDirectReceiver(WifiP2pManager wfdManager, WifiP2pManager.Channel wfdChannel, MainActivity appMainActivity){
        super();

        _wfdManager = wfdManager;
        _wfdChannel = wfdChannel;
        _appMainActivity = appMainActivity;
    }

    public void registerReceiver(){
        _appMainActivity.registerReceiver(this, getIntentFilter());
    }
    public void unregisterReceiver(){
        _appMainActivity.unregisterReceiver(this);
    }
    public boolean isWifiDirectEnabled(){
        return _isWifiDirectEnabled;
    }
    public WifiP2pDevice getFirstAvailableDevice(){
        return isWifiDirectEnabled() && _wfdDevices != null && _wfdDevices.length > 0 ? _wfdDevices[0] : null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

       // _appMainActivity.displayToast(action);

        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            handleWifiP2pStateChanged(intent);
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            handleWifiP2pThisDeviceChanged(intent);
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            handleWifiP2pPeersChanged(intent);
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            handleWifiP2pConnectionChanged(intent);
        }
    }

    private void handleWifiP2pStateChanged(Intent intent){
        int wfdState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        _isWifiDirectEnabled = wfdState == WifiP2pManager.WIFI_P2P_STATE_ENABLED ? true : false;
    }
    private void handleWifiP2pThisDeviceChanged(Intent intent){
        WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
    }
    private void handleWifiP2pPeersChanged(Intent intent){
        _wfdManager.requestPeers(_wfdChannel, this);
    }
    private void handleWifiP2pConnectionChanged(Intent intent){
        NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        if(info != null && info.isConnected()){
            _wfdManager.requestConnectionInfo(_wfdChannel, this);
        }
        else{
            _appMainActivity.displayToast("Connection Closed");
        }
    }

    private IntentFilter getIntentFilter(){
        if(_intentFilter == null){
            _intentFilter = new IntentFilter();
            _intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            _intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            _intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            _intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }
        return _intentFilter;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if(wifiP2pInfo.groupFormed){
            if(wifiP2pInfo.isGroupOwner){
                // Open server socket
            }
            else{
               // Open a socket to wifiP2pInfo.groupOwnerAddress;
            }
        }


    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        if(wifiP2pDeviceList != null && wifiP2pDeviceList.getDeviceList() != null && wifiP2pDeviceList.getDeviceList().size() > 0){
            _wfdDevices = wifiP2pDeviceList.getDeviceList().toArray(new WifiP2pDevice[0]);
        }
        else{
            _wfdDevices = null;
        }

    }
}
