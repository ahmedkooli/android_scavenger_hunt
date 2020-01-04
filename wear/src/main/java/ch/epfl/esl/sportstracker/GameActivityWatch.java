package ch.epfl.esl.sportstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class GameActivityWatch extends WearableActivity implements SensorEventListener {
    final static String RETRIEVE_MOBILE_INFO = "RETRIEVE_MOBILE_INFO";
    final static String GAME_INFO = "GAME_INFO";

    // vibration
    final static float VIBRATION_DISTANCE_THRESHOLD = 10.f; // the watch starts vibrating when player is within N meters from clue
    final static int VIBRATION_MAX_AMPLITUDE = 255;
    final static int VIBRATION_MAX_INTERVAL = 2000; // interval between two vibrations in milliseconds
    final static int VIBRATION_MIN_INTERVAL = 250;
    final static int VIBRATION_DURATION = 200;   // duration of the vibration
    Vibrator vibratore;

    private GameStatus game_status;
    private int last_distance_from_clue, distance_from_clue;
    private TextView coordinates_textview;
    private TextView heart_rate_textview;
    private TextView distance_from_clue_textview;

    // communication with mobile device through DataLayer
    private LocalBroadcastManager local_broadcast_manager;
    private BroadcastReceiver broadcast_receiver;

    // heart rate sensor
    private SensorManager sensor_manager;
    private Sensor heart_rate_sensor;
    private int heart_rate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // vibrations setup
        vibratore = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // text fields setup
        coordinates_textview = (TextView) findViewById(R.id.coordinates_textview);
        heart_rate_textview = (TextView) findViewById(R.id.heart_rate_textview);
        distance_from_clue_textview = (TextView) findViewById(R.id.distance_from_clue_textview);
        coordinates_textview.setText("Ciao");
        heart_rate_textview.setText("Please touch the sensor and stay still");
        distance_from_clue_textview.setText("Distance from clue:");

        // heart rate sensor setup
        checkBodySensorsPermission();
        setUpHeartRateSensor();

        // listens for messages from the mobile devices (updates of the game status)
        local_broadcast_manager = LocalBroadcastManager.getInstance(this);
        broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                game_status = (GameStatus) intent.getParcelableExtra(GAME_INFO);

                String coordinates_text = "Player Latitude: " + game_status.getPlayer_location().getLatitude() + "\nLongitude: " + game_status.getPlayer_location().getLongitude();
                coordinates_textview.setText(coordinates_text);

                distance_from_clue = (int) game_status.getPlayer_location().distanceTo(game_status.getNextClueLocation());
                if (distance_from_clue != last_distance_from_clue)
                {
                    last_distance_from_clue = distance_from_clue;
                    update_vibration(distance_from_clue);
                    distance_from_clue_textview.setText("Distance from clue: " + distance_from_clue + " m");
                }
            }
        };

        local_broadcast_manager.registerReceiver(broadcast_receiver, new IntentFilter(RETRIEVE_MOBILE_INFO));

        // Enables Always-on
        setAmbientEnabled();
    }



    public void update_vibration(float distance_from_clue)
    {
        if (distance_from_clue < VIBRATION_DISTANCE_THRESHOLD)
        {
            int vibration_amplitude =(int) (VIBRATION_MAX_AMPLITUDE * (VIBRATION_DISTANCE_THRESHOLD - distance_from_clue)/VIBRATION_DISTANCE_THRESHOLD);
            int vibration_interval = (int)(VIBRATION_MIN_INTERVAL + (VIBRATION_MAX_INTERVAL - VIBRATION_MIN_INTERVAL) * distance_from_clue / VIBRATION_DISTANCE_THRESHOLD);
            long[] vibration_timings = {VIBRATION_DURATION, vibration_interval};
            int[] vibration_amplitudes = {vibration_amplitude, 0};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect vibration_effect = VibrationEffect.createWaveform(vibration_timings, vibration_amplitudes, 0);
                vibratore.vibrate(vibration_effect);
            }
        }
        else
        {
            vibratore.cancel();
        }
    }

    public void sendHeartRate()
    {
        Intent intent_send_heart_rate = new Intent(GameActivityWatch.this, WearService.class);
        intent_send_heart_rate.setAction(WearService.ACTION_SEND.SEND_HEART_RATE.name());
        intent_send_heart_rate.putExtra(WearService.HEART_RATE, heart_rate);

        startService(intent_send_heart_rate);
    }

    @Override
    public void onDestroy() {
            super.onDestroy();
        local_broadcast_manager.unregisterReceiver(broadcast_receiver);
    }


    private void setUpHeartRateSensor()
    {
        sensor_manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        heart_rate_sensor = sensor_manager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensor_manager.registerListener(this, heart_rate_sensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void checkBodySensorsPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission("android.permission.BODY_SENSORS") == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission.BODY_SENSORS"}, 0);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            if (event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
            {
                heart_rate = (int)event.values[0];
                heart_rate_textview.setText("Heart rate: " + heart_rate);
                sendHeartRate();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        switch (accuracy)
        {
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                heart_rate_textview.setText("Hear rate sensor is calibrating... very low accuracy");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                heart_rate_textview.setText("Hear rate sensor is calibrating... medium accuracy");
                break;
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                heart_rate_textview.setText("Please touch the sensor and stay still");
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensor_manager.registerListener(this, heart_rate_sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensor_manager.unregisterListener(this);    // unregisters listener
        vibratore.cancel();     // stops vibration
    }


}
