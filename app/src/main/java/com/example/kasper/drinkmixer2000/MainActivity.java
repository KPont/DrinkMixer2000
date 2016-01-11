package com.example.kasper.drinkmixer2000;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener{

    private WifiP2pManager _wfdManager;
    private WifiP2pManager.Channel _wfdChannel;

    private WiFiDirectReceiver _wfdReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _wfdManager = (WifiP2pManager)getSystemService(WIFI_P2P_SERVICE);
        _wfdChannel = _wfdManager.initialize(this, getMainLooper(), this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    public void onClickMenuRegister(MenuItem item){
        registerWifiReceiver();
    }

    public void onClickMenuUnregister(MenuItem item){
        unregisterWfdReceiver();
    }

    public void onClickMenuDiscover(MenuItem item){
        if(isWfdReceiverRegisteredAndFeatureEnabled()){
            _wfdManager.discoverPeers(_wfdChannel, new ActionListenerHandler(this, "Discover Peers"));
        }
    }

    public void onClickMenuConnect(MenuItem item){
        if(isWfdReceiverRegisteredAndFeatureEnabled()){
            WifiP2pDevice theDevice = _wfdReceiver.getFirstAvailableDevice();
            if(theDevice != null){
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = theDevice.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                _wfdManager.connect(_wfdChannel, config, new ActionListenerHandler(this, "Connection"));
            }
            else {
                displayToast("No devices currently available");
            }
        }
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        unregisterWfdReceiver();
    }

    public void displayToast(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
    }

    private void registerWifiReceiver(){
        _wfdReceiver = new WiFiDirectReceiver(_wfdManager, _wfdChannel, this);
        _wfdReceiver.registerReceiver();
    }

    private void unregisterWfdReceiver(){
        if(_wfdReceiver != null){
            _wfdReceiver.unregisterReceiver();
            _wfdReceiver = null;
        }
    }

    private boolean isWfdReceiverRegisteredAndFeatureEnabled(){
        boolean isWfdUsable = _wfdReceiver != null && _wfdReceiver.isWifiDirectEnabled();

        if(!isWfdUsable){
            showWfdReceiverNotRegisteredOrFeatureNotEnabledMessage();
        }
        return isWfdUsable;
    }

    private void showWfdReceiverNotRegisteredOrFeatureNotEnabledMessage(){
        displayToast(_wfdReceiver == null ? "WifiDirect Broadcast Receiver Not Yet Registered" : "WifiDirect Not Enabled on Phone");
    }

    public void onClickMenuExit(MenuItem item){
        finish();
    }

    @Override
    public void onChannelDisconnected() {
        displayToast("Wifi Direct Channel Disconnected - Reinitializing");
        reinitializeChannel();
    }

    private void reinitializeChannel(){
        _wfdChannel = _wfdManager.initialize(this, getMainLooper(), this);
        if(_wfdChannel != null){
            displayToast("Wifi Direct Channel Initialization: SUCCESS");
        }
        else {
            displayToast("Wifi Direct Channel Initialization: FAILED");
        }
    }
}
