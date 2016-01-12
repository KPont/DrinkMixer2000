package com.example.kasper.drinkmixer2000;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by Alex on 12-01-2016.
 */
public class AccesGyroscope extends AppCompatActivity implements SensorEventListener {

    private TextView tv;
    private SensorManager sManager;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView)findViewById(R.id.tv);

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume(){
        super.onResume();

        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop(){
        sManager.unregisterListener(this);
        super.onStop();
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE){
            return;
        }
        tv.setText("Orientation X (Roll) :"+ Float.toString(event.values[2]) +"\n"+
                        "Orientation Y (Picht) :"+ Float.toString(event.values[1]) +"\n"+
        "Orientation Z (Yaw) :"+ Float.toString(event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
