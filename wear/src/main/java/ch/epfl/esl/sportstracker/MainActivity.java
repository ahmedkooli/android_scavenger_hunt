package ch.epfl.esl.sportstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends WearableActivity {

    private final String TAG = this.getClass().getSimpleName();

    public static final String ACTION_RECEIVE_PROFILE_INFO = "RECEIVE_PROFILE_INFO";
    public static final String PROFILE_IMAGE = "PROFILE_IMAGE";
    public static final String PROFILE_USERNAME = "PROFILE_USERNAME";
    private ConstraintLayout mLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ImageView imageView = findViewById(R.id.wearImageView);
                TextView textView = findViewById(R.id.wearTextView);

                byte[] byteArray = intent.getByteArrayExtra(PROFILE_IMAGE);
                Bitmap bmpProfile = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imageView.setImageBitmap(bmpProfile);

                String username = intent.getStringExtra(PROFILE_USERNAME);
                textView.setText(String.format(getString(R.string.welcome_name), username));

            }
        }, new IntentFilter(ACTION_RECEIVE_PROFILE_INFO));

        mLayout = findViewById(R.id.container);
        // Enables Always-on
        setAmbientEnabled();


    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        updateDisplay();
    }

    private void updateDisplay() {
        if(isAmbient()){
            mLayout.setBackgroundColor(getResources().getColor(android.R.color.black,getTheme()));
        }else{
            mLayout.setBackgroundColor(getResources().getColor(android.R.color.white,getTheme()));
        }
    }
}
