package com.example.kasper.drinkmixer2000;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener{

    private WifiP2pManager _wfdManager;
    private WifiP2pManager.Channel _wfdChannel;

    private WiFiDirectReceiver _wfdReceiver;

    private TextView text;

    ServerSocket serverSocket;

    Handler updateConversationHandler;

    Thread serverThread = null;
    private Socket socket;
    private static int serverPortC = 5000;
    private static String serverIP = "10.0.2.2";

    public static final int serverPort = 6000;

    public void setServerIP(String IP){
        this.serverIP = IP;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text2);

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

    public void beginThread(){
        serverThread = new Thread(new ServerThread());
        updateConversationHandler = new Handler();
        this.serverThread.start();
    }

    class updateUIThread implements Runnable{
        private String msg;

        public updateUIThread(String str){
            this.msg = str;
        }
        @Override
        public void run() {
            text.setText(text.getText().toString()+"Client Says: " + msg + "\n");
        }
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {
            Socket socketServ = null;
            try{
                serverSocket = new ServerSocket(serverPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while(!Thread.currentThread().isInterrupted()){
                try{
                    socketServ = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socketServ);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class CommunicationThread implements Runnable{
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket){
            this.clientSocket = clientSocket;

            try{
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try{
                    String read = input.readLine();

                    updateConversationHandler.post(new updateUIThread(read));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void beginClient(){
        new Thread(new ClientThread()).start();

    }
    public void onClick(View view) {

        try {
            EditText et = (EditText) findViewById(R.id.EditText01);

            String str = et.getText().toString();

            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),

            true);

            out.println(str);

        } catch (UnknownHostException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }
    class ClientThread implements Runnable{


        @Override
        public void run() {
            try {

                InetAddress serverAddr = InetAddress.getByName(serverIP);



                socket = new Socket(serverAddr, serverPort);



            } catch (UnknownHostException e1) {

                e1.printStackTrace();

            } catch (IOException e1) {

                e1.printStackTrace();

            }

        }
    }

}
