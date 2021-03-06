package com.example.kasper.drinkmixer2000;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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


public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, SensorEventListener{

    private String drinkName = "";
    private String juice = "0";
    private String vodka = "0";
    private String cola = "0";

    private WifiP2pManager _wfdManager;
    private WifiP2pManager.Channel _wfdChannel;

    private Sensor accelerometer;
    private SensorManager sManager;

    private TextView tv;
    private TextView text;
    private TextView ingredient;
    private TextView data;
    private TextView jCl;
    private TextView vCl;
    private TextView cCl;

    private EditText cl;

    private boolean juicePressed = false;
    private boolean vodkaPressed = false;
    private boolean colaPressed = false;
    private boolean pouring = false;
    private boolean mixing = false;
    private boolean pourPressed = false;

    private Button juiceButton;
    private Button vodkaButton;
    private Button colaButton;
    private Button addBtn;

    private String read;

    private WiFiDirectReceiver _wfdReceiver;
    private WifiP2pConfig config;



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
        config = new WifiP2pConfig();
        config.groupOwnerIntent = 15;

        juiceButton = (Button) findViewById(R.id.btnJuice);
        vodkaButton = (Button) findViewById(R.id.btnVodka);
        colaButton = (Button) findViewById(R.id.btnCola);
        addBtn = (Button) findViewById(R.id.btnAddCl);

        ingredient = (TextView) findViewById(R.id.ingChoosen);
        text = (TextView) findViewById(R.id.text2);
        tv = (TextView)findViewById(R.id.tv);
        data = (TextView)findViewById(R.id.Datatext);
        cl = (EditText)findViewById(R.id.editTextCL);
        jCl = (TextView)findViewById(R.id.textViewClJuice);
        vCl = (TextView)findViewById(R.id.textViewClVodka);
        cCl = (TextView)findViewById(R.id.textViewClCola);


        _wfdManager = (WifiP2pManager)getSystemService(WIFI_P2P_SERVICE);
        _wfdChannel = _wfdManager.initialize(this, getMainLooper(), this);

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
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
        config.groupOwnerIntent = 0;
        if(isWfdReceiverRegisteredAndFeatureEnabled()){
            WifiP2pDevice theDevice = _wfdReceiver.getFirstAvailableDevice();
            if(theDevice != null){

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
            text.setText(text.getText().toString()+"Device Says: " + msg + "\n");
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
                        read = input.readLine();


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

    public void sendData(String dataToSend){
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.println(dataToSend);
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
    public void onClickSensor(View v) {

        if (!pourPressed) {
            pourPressed = true;
            data.setText("Pouring mode ON");

            juiceButton.setVisibility(View.VISIBLE);
            vodkaButton.setVisibility(View.VISIBLE);
            colaButton.setVisibility(View.VISIBLE);
            cl.setVisibility(View.GONE);
            addBtn.setVisibility(View.GONE);

            sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            accelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (pourPressed){
            sManager.unregisterListener(this);

            pourPressed = false;
            juiceButton.setVisibility(View.GONE);
            vodkaButton.setVisibility(View.GONE);
            colaButton.setVisibility(View.GONE);
            ingredient.setText("");
            tv.setText("");
            data.setText("Pouring mode OFF");
        }
    }
    public void onClickMix(View v) {
        if (!mixing) {
            mixing = true;
            juiceButton.setVisibility(View.VISIBLE);
            vodkaButton.setVisibility(View.VISIBLE);
            colaButton.setVisibility(View.VISIBLE);
            cl.setVisibility(View.VISIBLE);
            addBtn.setVisibility(View.VISIBLE);
        }
        else if (mixing){
            mixing = false;
            sendData(juice + "," + vodka + "," + cola);
            juiceButton.setVisibility(View.GONE);
            vodkaButton.setVisibility(View.GONE);
            colaButton.setVisibility(View.GONE);
            cl.setVisibility(View.GONE);
            addBtn.setVisibility(View.GONE);
            ingredient.setText("");
            juice = "0";
            jCl.setText("");
            vodka = "0";
            vCl.setText("");
            cola = "0";
            cCl.setText("");

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Float x = event.values[0];
        Float y = event.values[1];
        Float z = event.values[2];

        tv.setText("X: " + x +
                "\nY: " + y +
                "\nZ: " + z);

        if (x < 1 && x > -1 && y < 1 && y > -1 && z > 9){
            sManager.unregisterListener(this);

            pourPressed = false;
            juiceButton.setVisibility(View.GONE);
            vodkaButton.setVisibility(View.GONE);
            colaButton.setVisibility(View.GONE);
            ingredient.setText("");
            tv.setText("");

            data.setText("Pouring mode OFF");
        }
        if (x > 7 && y < 7 && z < 3 && z > -3 && !pouring && juicePressed){
            pouring = true;
            data.setText("Pouring Juice...");
            sendData("1");
        }
        if (x > 7 && y < 7 && z < 3 && z > -3 && !pouring && vodkaPressed){
            pouring = true;
            data.setText("Pouring Vodka...");
            sendData("2");
        }
        if (x > 7 && y < 7 && z < 3 && z > -3 && !pouring && colaPressed){
            pouring = true;
            data.setText("Pouring Cola...");
            sendData("3");
        }
        if (x < 5 && y > 9 && z < 3 && z > -3 && pouring){
            pouring = false;
            data.setText("Stopped pouring");
            sendData("#");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onClickJuice(View v){
        ingredient.setText("Choosen: Juice");
        vodkaPressed = false;
        colaPressed = false;
        juicePressed = true;
    }
    public void onClickVodka(View v){
        ingredient.setText("Choosen: Vodka");
        juicePressed = false;
        colaPressed = false;
        vodkaPressed = true;
    }
    public void onClickCola(View v){
        ingredient.setText("Choosen: Cola");
        juicePressed = false;
        vodkaPressed = false;
        colaPressed = true;
    }
    public void onClickAddCl(View v){
        String centilitres = cl.getText().toString();
        cl.setText("");
        if (juicePressed){
            juice = centilitres;
            jCl.setText("Cl: "+juice);
        }
        if (vodkaPressed){
            vodka = centilitres;
            vCl.setText("Cl: "+vodka);
        }
        if (colaPressed){
            cola = centilitres;
            cCl.setText("Cl: "+cola);
        }

    }


}
