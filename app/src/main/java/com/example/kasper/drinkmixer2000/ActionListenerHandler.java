package com.example.kasper.drinkmixer2000;

import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by Kasper on 11-01-2016.
 */
public class ActionListenerHandler implements WifiP2pManager.ActionListener{
    MainActivity _activity;
    String _actionDisplayText;
    public ActionListenerHandler(MainActivity activity, String actionDisplayText){
        _activity = activity;
        _actionDisplayText = actionDisplayText;
    }

    @Override
    public void onSuccess() {
        _activity.displayToast(_actionDisplayText + " Started");
    }

    @Override
    public void onFailure(int reason) {
        _activity.displayToast(_actionDisplayText + " Failed");
    }
}
