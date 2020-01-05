package ch.epfl.esl.sportstracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.wearable.DataMap;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;




public class GameActivity extends AppCompatActivity implements OnMapReadyCallback{
    // used for the map
    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String ICON_ID = "ICON_ID";
    private static final String LAYER_ID = "LAYER_ID";
    private static final String CLUES_SOURCE_ID = "CLUES_SOURCE_ID";
    private static final String CLUES_ICON_ID = "CLUES_ICON_ID";
    private static final String CLUES_LAYER_ID = "CLUES_LAYER_ID";
    private MapView mapView;
    private GeoJsonSource geoJsonSource_treasures_coordinates;  // this object holds the position of the markers shown on the map (the icons of the little treasures)
    private GeoJsonSource geoJsonSource_clues_coordinates;

    // used for the Augmented Reality
    private ArFragment arFragment;

    // stores all game objects and settings
    private Game game = new Game();

    // localization
    private FusedLocationProviderClient fused_location_client;
    private LocationCallback location_callback;
    final static long LOCATION_REQUEST_INTERVAL = 500;  // asks the system the location every tot milliseconds

    // communication with watch through DataLayer
    static final String RETRIEVE_WATCH_INFO = "RETRIEVE_WATCH_INFO";
    static final String HEART_RATE = "HEART_RATE";
    static final int DEFAULT_HEART_RATE = 0;
    private LocalBroadcastManager local_broadcast_manager;
    private BroadcastReceiver broadcast_receiver;

    // FOR TESTING PURPOSES:
    private TextView text, heart_rate_textview, next_clue_location_textview, time_textView;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token)); // THIS MUST BE CALLED BEFORE "setContentView()"
        setContentView(R.layout.activity_game);

        // FOR TESTING PURPOSES: textViews setup
        text = (TextView) findViewById(R.id.player_location_textView);
        heart_rate_textview = (TextView) findViewById(R.id.heart_rate_textView);
        next_clue_location_textview = (TextView) findViewById(R.id.next_clue_location_textView);
        time_textView = (TextView) findViewById(R.id.time_textView);

        // setup of the map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // here we set the callback function to be called whenever we receive an update on the location
        setupLocationSensor();

        // here we set the callback function to be called whenever we receive an update from the watch
        setupCommunicationWithWatch();

        // here we load all game objects (players, treasures, etc..) from Firebase ---TO BE DONE!!!
        setupGame();

        // here we start a function which updates the game every tot milliseconds
        startGameLoop();

        /*arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // Executed when user taps on plane (detected by arFragment)
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            // Describe fixed location and orientation in real world
            // 3D model displayed on top of anchor
            Anchor anchor = hitResult.createAnchor();

            // Build the model
            ModelRenderable.builder()
                    .setSource(this, Uri.parse("model.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
                    .exceptionally(throwable -> {
                        // If an error happens
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(throwable.getMessage())
                                .show();
                        return null;
                    });
        }));*/
   }


    /** game loop, updates the game every tot milliseconds */
    public void startGameLoop()
    {
        final Handler handler = new Handler();
        final int delay = 500; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){

                updateGame();
                updateMap();

                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    /** sends the current status of the game to the watch (only relevant information, we can customize the info sent by modifying the "toDataMap()" method in Game )*/
    public void SendGameStatusToWatch()
    {
        // puts relevant data into a dataMap object
        DataMap data = game.toDataMap();

        // sends the data to the service in charge of the communication with the watch
        Intent intent = new Intent(GameActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.SEND_GAME_STATUS.name());
        intent.putExtra(WearService.GAME_STATUS, data.toBundle());  // we need to put the data into a Bundle send it through the intent

        startService(intent);
    }


    /** FOR TESTING PURPOSES: creates a new treasure at the current location and sets it as the next treasure to find*/
    public void buttonClick(View view)
    {
        ObjectAR new_treasure = new ObjectAR(game.getPlayerLocation());
        game.setTreasureToFind(new_treasure);
        SendGameStatusToWatch();
    }




    /** tells the system to send us updates on the location every tot milliseconds*/
    private void startLocationUpdates()
    {
        LocationRequest location_request = new LocationRequest();
        location_request.setInterval(LOCATION_REQUEST_INTERVAL);
        location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fused_location_client.requestLocationUpdates(   location_request,
                location_callback,
                Looper.getMainLooper());
    }

    /** tells the system to stop sending us updates on the location*/
    private void stopLocationUpdates()
    {
        fused_location_client.removeLocationUpdates(location_callback);
    }


    /** Here we define what to do when we receive data from the watch (for now we receive just heart rate)*/
    private void setupCommunicationWithWatch()
    {
        // Register for updates on data sent from the watch
        local_broadcast_manager = LocalBroadcastManager.getInstance(this);
        broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int heart_rate;
                heart_rate = intent.getIntExtra(HEART_RATE, DEFAULT_HEART_RATE);
                game.setHeartRate(heart_rate);

                // FOR TESTING PURPOSES:
                heart_rate_textview.setText("Heart rate: " + heart_rate);   // shows heart rate received from watch
            }
        };
        local_broadcast_manager.registerReceiver(broadcast_receiver, new IntentFilter(RETRIEVE_WATCH_INFO));
    }

    /** Here we define what to do when we receive updates on the location*/
    private void setupLocationSensor()
    {
        checkLocationPermissions();
        checkLocationEnabled();
        fused_location_client = new FusedLocationProviderClient(this);
        location_callback = new LocationCallback()
        {
            //  when it receives a new location from the system it updates game status and sends it to the watch
            @Override
            public void onLocationResult(LocationResult location_result)
            {
                if (location_result != null)
                {
                    for(Location location : location_result.getLocations())
                    {
                        // updates player location
                        game.setPlayerLocation(location);

                        // sends the updated game status to the watch
                        SendGameStatusToWatch();
                    }
                }
            }
        };
        startLocationUpdates();
    }

    /** Checks if location is on and if not asks user to turn it on */
    private void checkLocationEnabled () {
        LocationManager location_manager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = location_manager.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = location_manager.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(GameActivity. this )
                    .setMessage( "Please enable localization" )
                    .setPositiveButton( "Settings" , new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                }
                            })
                    .setNegativeButton( "Cancel" , null )
                    .show() ;
        }
    }

    /** Checks if user has allowed access to location and prompts to do so */
    private void checkLocationPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission("android" + ""
                + ".permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") ==
                        PackageManager.PERMISSION_DENIED || checkSelfPermission("android" + "" +
                ".permission.INTERNET") == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android"
                    + ".permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"}, 0);
        }
    }


    /** loads all information useful for the game from Firebase
     * TO DO: LOAD PLAYER, OPPONENT, MAP AND SETTINGS FROM FIREBASE!!*/
    private void setupGame()
    {
        //TO DO: load map, opponent and other info from Firebase!!! These are just dummy values
        Player opponent = new Player();
        GameMap map_rolex = new GameMap();
        map_rolex.addTreasure(new ObjectAR(46.519039, 6.567473));
        map_rolex.addTreasure(new ObjectAR(46.518392, 6.566816));
        //map_rolex.addTreasure(new ObjectAR(46.518924, 6.566398));
        //map_rolex.addTreasure(new ObjectAR(46.518437, 6.568072));
        map_rolex.addClue (new ObjectAR(46.518924, 6.566398));
        map_rolex.addClue (new ObjectAR(46.518437, 6.568072));
        map_rolex.setName("Rolex Map");

        ObjectAR treasure_to_find = map_rolex.getTreasure(0);   // the treasure to find is the first in the list

        game.addOpponent(opponent);
        game.setMap(map_rolex);
        game.setTreasureToFind(treasure_to_find);
        game.setStartingTime(System.currentTimeMillis());
        game.setTimeLimit(System.currentTimeMillis() + 10000);
    }


    /** Runs the game logic according to the game data. It is called from "gameLoop" every 0.5 seconds
     *  PUT ALL GAME LOGIC HERE */
    private long last_remaining_time;
    private void updateGame()
    {
        Toast toast;
        long remaining_time = game.getRemainingTime();

        // shows time limit
        String formatted_text = "Time remaining: " + game.getRemainingTime() / 1000 + "\nTime elapsed: " + game.getElapsedTime() / 1000;
        time_textView.setText(formatted_text);    // shows remaining and elapsed time

        // displays player location on the screen
        formatted_text = "Player\n" + game.getPlayer().getLocation().getLatitude() +  " - " + game.getPlayer().getLocation().getLongitude();
        text.setText(formatted_text);

        // shows on the screen the position of the next treasure and its distance from the player
        formatted_text = "Treasure\n" + game.getTreasureToFind().getLocation().getLatitude() +  " - " + game.getTreasureToFind().getLocation().getLongitude();
        formatted_text += "\nDistance: " + (int)game.getPlayerDistanceFromTreasure();
        next_clue_location_textview.setText(formatted_text);

        //  if the player founds a treasure
        if (game.getPlayerDistanceFromTreasure() < game.threshold_distance )
        {
            game.getPlayer().addTreasureFound(game.getTreasureToFind());
            // TO DO: update Firebase
        }

        // sends alerts when time is running out
        if ((remaining_time < 60000)&&(last_remaining_time > 60000))    // checks for transition through 60 seconds
        {
            toast=Toast.makeText(this, R.string.one_minute_alert, Toast.LENGTH_LONG);
            toast.show();
        }
        else if ((remaining_time < 30000)&&(last_remaining_time > 30000))
        {
            toast=Toast.makeText(this, R.string.thirty_seconds_alert, Toast.LENGTH_LONG);
            toast.show();
        }
        else if ((remaining_time < 10000)&&(last_remaining_time > 10000))
        {
            toast=Toast.makeText(this, R.string.ten_seconds_alert, Toast.LENGTH_SHORT);
            toast.show();
        }
        // if time is off sends message to player and sets the next treasure to find
        if (remaining_time <= 0)
        {
            toast = Toast.makeText(this, R.string.msg_treasure_fail, Toast.LENGTH_SHORT);
            toast.show();

            // finds index of current treasure
            int current_treasure_index = game.getMap().getTreasures().indexOf(game.getTreasureToFind());

            // if there are still treasure after the current one:
            if (current_treasure_index < game.getMap().getTreasuresCount() - 1)
            {
                // sets next treasure
                ObjectAR next_treasure = game.getMap().getTreasure(current_treasure_index + 1);
                game.setTreasureToFind(next_treasure);

                // updates time limit according to distance from treasure
                game.setTimeLimit( System.currentTimeMillis() + game.min_available_time +
                                        (long)(game.bonus_ms_per_meter * game.getPlayerDistanceFromTreasure()));
                remaining_time = game.getRemainingTime();
            }
            else
            {
                // TO DO: game finished
                toast = Toast.makeText(this, R.string.msg_game_finished, Toast.LENGTH_SHORT);
                toast.show();
            }



        }
        last_remaining_time = remaining_time;
    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        // Automatically positions itself on given anchor (cannot be changed)
        AnchorNode anchorNode = new AnchorNode(anchor);

        // Node that can be changed (position or zoom)
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        // Place it where anchorNode is
        transformableNode.setParent(anchorNode);
        // Give it the model
        transformableNode.setRenderable(modelRenderable);

        // Add to scene
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        // Select transformableNode
        transformableNode.select();
    }


    /** This function runs when the map has loaded and is ready to work. It sets the appearance of
     * the map and of the markers that indicate the treasures.*/
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        // this object will contain the markers coordinates (can be modified and markers will update)
        geoJsonSource_treasures_coordinates = new GeoJsonSource(SOURCE_ID);
        geoJsonSource_clues_coordinates = new GeoJsonSource(CLUES_SOURCE_ID);

        mapboxMap.setStyle(Style.OUTDOORS, style -> {
            /* This function runs after the style has been set. The map is set up and the style has loaded.
                Now you can add additional data or make other map adjustments.*/

            // sets markers icon
            style.addImage(ICON_ID, BitmapFactory.decodeResource(getResources(), R.drawable.treasure));

            // set the source for the icons coordinates
            style.addSource(geoJsonSource_treasures_coordinates);

            style.addLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                    .withProperties(
                            PropertyFactory.iconImage(ICON_ID),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconSize(0.05f),
                            PropertyFactory.iconAllowOverlap(true)));

            // sets clues icon
            style.addImage(CLUES_ICON_ID, BitmapFactory.decodeResource(getResources(), R.drawable.icon_clue));

            // set the source for the icons coordinates
            style.addSource(geoJsonSource_clues_coordinates);

            style.addLayer(new SymbolLayer(CLUES_LAYER_ID, CLUES_SOURCE_ID)
                    .withProperties(
                            PropertyFactory.iconImage(CLUES_ICON_ID),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconSize(0.05f),
                            PropertyFactory.iconAllowOverlap(true)));
        });
    }


    @Override
    public void onResume()
    {
        super.onResume();
        startLocationUpdates();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        stopLocationUpdates();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /** Updates the treasure marker displayed on the map according to the treasures stored in "game".
     * It uses the object "geoJsonSource_treasures_coordinates" which contains the treasure markers coordinates.
     * If we modify it, the treasure markers on the map are updated.*/
    public void updateMap()
    {

        // We need to check if it's "null" because it is initialized only when the map has fully loaded
        if (geoJsonSource_treasures_coordinates != null)
        {
            // creates a list containing the treasures coordinates extracted from game
            List<Feature> features_treasures_coordinates = new ArrayList<>();

            for(ObjectAR treasure : game.getPlayer().getTrasuresFound())
            {
                double latitude, longitude;
                latitude = treasure.getLatitude();
                longitude = treasure.getLongitude();
                Point point = Point.fromLngLat(longitude, latitude);
                Feature feature = Feature.fromGeometry(point);
                features_treasures_coordinates.add(feature);
            }

            // updates the position of the treasure icons on the map
            geoJsonSource_treasures_coordinates.setGeoJson(FeatureCollection.fromFeatures(features_treasures_coordinates));
        }

        if (geoJsonSource_clues_coordinates != null)
        {
            // creates a list containing the treasures coordinates extracted from game
            List<Feature> features_clues_coordinates = new ArrayList<>();

            for(ObjectAR clue : game.getMap().getClues())
            {
                double latitude, longitude;
                latitude = clue.getLatitude();
                longitude = clue.getLongitude();
                Point point = Point.fromLngLat(longitude, latitude);
                Feature feature = Feature.fromGeometry(point);
                features_clues_coordinates.add(feature);
            }

            // updates the position of the treasure icons on the map
            geoJsonSource_clues_coordinates.setGeoJson(FeatureCollection.fromFeatures(features_clues_coordinates));
        }
    }
}




