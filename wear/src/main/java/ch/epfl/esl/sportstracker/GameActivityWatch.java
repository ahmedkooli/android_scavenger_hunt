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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.widget.Toast;

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

    private GameStatus game_status = new GameStatus();
    private long last_remaining_time;
    private int last_distance_from_treasure;
    boolean timer_running = false;
    private TextView time_textview;
    private TextView heart_rate_textview;
    private TextView distance_from_treasure_textview;

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
        time_textview = (TextView) findViewById(R.id.time_textview);
        heart_rate_textview = (TextView) findViewById(R.id.heart_rate_textview);
        distance_from_treasure_textview = (TextView) findViewById(R.id.distance_from_treasure_textview);
        time_textview.setText("...");
        heart_rate_textview.setText("...");
        distance_from_treasure_textview.setText("...");

        // heart rate sensor setup
        checkBodySensorsPermission();
        setUpHeartRateSensor();

        // listens for messages from the mobile devices (updates of the game status)
        local_broadcast_manager = LocalBroadcastManager.getInstance(this);
        broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                game_status = (GameStatus) intent.getParcelableExtra(GAME_INFO);

                if (timer_running == false) {
                    timer_running = true;
                    new CountDownTimer(game_status.getRemainingTime(), 1000) {

                        public void onTick(long millisUntilFinished) {
                            updateGame();
                        }

                        public void onFinish() {
                            timer_running = false;
                        }
                    }.start();
                }
            }
        };

        local_broadcast_manager.registerReceiver(broadcast_receiver, new IntentFilter(RETRIEVE_MOBILE_INFO));

        // Enables Always-on
        setAmbientEnabled();
    }



    public void updateGame()
    {
        long remaining_time = game_status.getRemainingTime();
        Toast toast;

        // builds a vibration pattern for alerts
        long[] vibration_timings = {200, 100, 200};
        int[] vibration_amplitudes = {VIBRATION_MAX_AMPLITUDE, 0, VIBRATION_MAX_AMPLITUDE};
        VibrationEffect alert_vibration_pattern = VibrationEffect.createWaveform(vibration_timings, vibration_amplitudes, -1);

        // computes the distance from the treasure and displays it
        int distance_from_treasure = (int) game_status.getPlayer_location().distanceTo(game_status.getNextClueLocation());
        distance_from_treasure_textview.setText( distance_from_treasure + " meters");

        // displays remaining time on the screen
        time_textview.setText(game_status.getRemainingTime() /1000 + "s");
        time_textview.setText(game_status.getTimeLimit() +"");

        // checks if we are getting close to the treasure: if yes vibrates
        if ((distance_from_treasure < 50)&&(last_distance_from_treasure >= 50))
        {
            vibratore.vibrate(alert_vibration_pattern);
            toast=Toast.makeText(this, "Quite close!!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if ((distance_from_treasure < 20)&&(last_distance_from_treasure >= 20))
        {
            vibratore.vibrate(alert_vibration_pattern);
            toast=Toast.makeText(this, "Very close!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if ((distance_from_treasure < 10)&&(last_distance_from_treasure >= 10))
        {
            vibratore.vibrate(alert_vibration_pattern);
            toast=Toast.makeText(this, "Almost there!", Toast.LENGTH_SHORT);
            toast.show();
        }


        // sends alerts when time is running out
        if ((remaining_time < 60000)&&(last_remaining_time >= 60000))    // checks for transition through 60 seconds
        {
            vibratore.vibrate(alert_vibration_pattern);
            toast=Toast.makeText(this, "1 minute!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if ((remaining_time < 30000)&&(last_remaining_time >= 30000))
        {
            vibratore.vibrate(alert_vibration_pattern);
            toast=Toast.makeText(this, "30 secs!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if ((remaining_time < 10000)&&(last_remaining_time >= 10000))
        {
            vibratore.vibrate(alert_vibration_pattern);
            toast=Toast.makeText(this, "10 secs!", Toast.LENGTH_SHORT);
            toast.show();
        }

        last_remaining_time = remaining_time;
        last_distance_from_treasure = distance_from_treasure;
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
                heart_rate_textview.setText("Low accuracy...");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                heart_rate_textview.setText("Medium accuracy...");
                break;
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                heart_rate_textview.setText("No contact");
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
