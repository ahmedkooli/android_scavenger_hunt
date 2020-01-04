package ch.epfl.esl.sportstracker;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearService extends WearableListenerService {

    // Tag for Logcat
    private static final String TAG = "WearService";
    private static final int DEFAULT_HEART_RATE = 0;

    public static final String HEART_RATE = "HEART_RATE";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // If no action defined, return
        if (intent.getAction() == null) return START_NOT_STICKY;

        // Match against the given action
        ACTION_SEND action = ACTION_SEND.valueOf(intent.getAction());
        PutDataMapRequest putDataMapRequest;

        switch (action) {
            case SEND_HEART_RATE:
                // gets the heart rate from the intent which started this service
                int heart_rate = intent.getIntExtra(HEART_RATE, DEFAULT_HEART_RATE);

                // puts the heart rate into a DataMap object
                DataMap heart_rate_data_map = new DataMap();
                heart_rate_data_map.putInt(BuildConfig.W_heart_rate, heart_rate);

                // sends the DataMap to the DataLayer
                putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_send_heart_rate);
                putDataMapRequest.getDataMap().putDataMap(BuildConfig.W_send_heart_rate, heart_rate_data_map);
                sendPutDataMapRequest(putDataMapRequest);
                break;
            default:
                Log.w(TAG, "Unknown action");
                break;
        }

        return START_NOT_STICKY;
    }



    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {

            // Get the URI of the event
            Uri uri = event.getDataItem().getUri();

            // Test if data has changed or has been removed
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                // Extract the dataMap from the event
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                Log.v(TAG, "DataItem Changed: " + event.getDataItem().toString() + "\n"
                        + "\tPath: " + uri
                        + "\tDatamap: " + dataMapItem.getDataMap() + "\n");

                Intent intent;

                assert uri.getPath() != null;
                switch (uri.getPath()) {
                    case BuildConfig.W_send_game_info:
                        GameStatus game_info = new GameStatus();
                        // Extracts location of the player
                        DataMap data = dataMapItem.getDataMap().getDataMap(BuildConfig.W_send_game_info);
                        double latitude = data.getDouble(BuildConfig.W_player_latitude);
                        double longitude = data.getDouble(BuildConfig.W_player_longitude);
                        game_info.setPlayerLocation(latitude, longitude);

                        // Extracts location of the next clue
                        latitude = data.getDouble(BuildConfig.W_next_clue_latitude);
                        longitude = data.getDouble(BuildConfig.W_next_clue_longitude);
                        game_info.setNextClueLocation(latitude, longitude);

                        // Extracts time limit
                        long time_limit;
                        time_limit = data.getLong(BuildConfig.W_time_limit);
                        game_info.setTimeLimit(time_limit);

                        // sends game status to GameActivityWatch through an intent
                        intent = new Intent(GameActivityWatch.RETRIEVE_MOBILE_INFO);
                        intent.putExtra(GameActivityWatch.GAME_INFO, game_info);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        break;
                    default:
                        Log.v(TAG, "Data changed for unhandled path: " + uri);
                        break;
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.w(TAG, "DataItem deleted: " + event.getDataItem().toString());
            }
        }
    }





    void sendPutDataMapRequest(PutDataMapRequest putDataMapRequest) {
        putDataMapRequest.getDataMap().putLong("time", System.nanoTime());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();
        Wearable.getDataClient(this)
                .putDataItem(request)
                .addOnSuccessListener(new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.v(TAG, "Sent datamap.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Datamap not sent. " + e.getMessage());
                    }
                });
    }



    // Constants
    public enum ACTION_SEND {
        SEND_HEART_RATE
    }
}
