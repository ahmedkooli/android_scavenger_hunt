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
    private MapView mapView;

    // used for the Augmented Reality
    private ArFragment arFragment;

    // stores all game objects and settings
    private GameData game_data = new GameData();

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
    private TextView text, heart_rate_textview, next_clue_location_textview, time_elapsed_textView;

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
        time_elapsed_textView = (TextView) findViewById(R.id.time_elapsed_textView);

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


    // game loop, updates the game every tot milliseconds
    public void startGameLoop()
    {
        final Handler handler = new Handler();
        final int delay = 500; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){

                updateGame();

                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    // sends the current status of the game_data to the watch (only relevant information, we can customize the info sent by modifying the "toDataMap()" method in GameData )
    public void SendGameStatusToWatch()
    {
        // puts relevant data into a dataMap object
        DataMap data = game_data.toDataMap();

        // sends the data to the service in charge of the communication with the watch
        Intent intent = new Intent(GameActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.SEND_GAME_STATUS.name());
        intent.putExtra(WearService.GAME_STATUS, data.toBundle());  // we need to put the data into a Bundle send it through the intent

        startService(intent);
    }


    // FOR TESTING PURPOSES: creates a new treasure at the current location and sets it as the next treasure to find
    public void buttonClick(View view)
    {
        Treasure new_treasure = new Treasure(game_data.getPlayerLocation());
        game_data.setNext_treasure(new_treasure);
        SendGameStatusToWatch();
    }





    // tells the system to send us updates on the location every tot milliseconds
    private void startLocationUpdates()
    {
        LocationRequest location_request = new LocationRequest();
        location_request.setInterval(LOCATION_REQUEST_INTERVAL);
        location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fused_location_client.requestLocationUpdates(   location_request,
                location_callback,
                Looper.getMainLooper());
    }

    // tells the system to stop sending us updates on the location
    private void stopLocationUpdates()
    {
        fused_location_client.removeLocationUpdates(location_callback);
    }


    // defines what to do when we receive data from the watch (for now we receive just heart rate)
    private void setupCommunicationWithWatch()
    {
        // Register for updates on data sent from the watch
        local_broadcast_manager = LocalBroadcastManager.getInstance(this);
        broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int heart_rate;
                heart_rate = intent.getIntExtra(HEART_RATE, DEFAULT_HEART_RATE);
                game_data.setHeartRate(heart_rate);

                // FOR TESTING PURPOSES:
                heart_rate_textview.setText("Heart rate: " + heart_rate);   // shows heart rate received from watch
            }
        };
        local_broadcast_manager.registerReceiver(broadcast_receiver, new IntentFilter(RETRIEVE_WATCH_INFO));
    }

    // defines what to do when we receive updates on the location
    private void setupLocationSensor()
    {
        checkLocationPermissions();
        checkLocationEnabled();
        fused_location_client = new FusedLocationProviderClient(this);
        location_callback = new LocationCallback()
        {
            //  when it receives a new location from the system it updates game_data status and sends it to the watch
            @Override
            public void onLocationResult(LocationResult location_result)
            {
                if (location_result != null)
                {
                    for(Location location : location_result.getLocations())
                    {
                        // updates player location
                        game_data.setPlayerLocation(location);

                        // sends the updated game_data status to the watch
                        SendGameStatusToWatch();

                        // FOR TESTING PURPOSES: displays player location on the screen
                        String formatted_text;
                        formatted_text = "Latitude: " + game_data.getPlayer().getLocation().getLatitude() +  "   Longitude: " + game_data.getPlayer().getLocation().getLongitude();
                        text.setText("Player:  " + formatted_text);

                        // shows on the screen the position of the next clue and its distance from the player
                        formatted_text = "Latitude: " + game_data.getNext_treasure().getLocation().getLatitude() +  "   Longitude: " + game_data.getNext_treasure().getLocation().getLongitude();
                        formatted_text += "    Distance: " + game_data.getPlayerDistanceFromTreasure();
                        next_clue_location_textview.setText(formatted_text);
                    }
                }
            }
        };
        startLocationUpdates();
    }

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

    // loads all information useful for the game from Firebase
    private void setupGame()
    {
        //TO DO: load treasures, opponent and other info from Firebase!!!
        Player opponent = new Player();
        ArrayList<Treasure> treasures = new ArrayList<Treasure>();
        Treasure next_treasure = new Treasure();
        long time_limit = 0;
        float threshold_distance = 0;

        game_data.addOpponent(opponent);
        game_data.setTreasures(treasures);
        game_data.setNext_treasure(next_treasure);
        game_data.setStarting_time(System.currentTimeMillis());
        game_data.setTime_limit(time_limit);
        game_data.setThreshold_distance(threshold_distance);
    }

    // runs the game logic according to the game data
    private void updateGame()
    {
        // FOR TESTING PURPOSES:
        time_elapsed_textView.setText("Time: " + game_data.getElapsedTime() / 1000);    // shows elapsed time

        //  if the player founds a treasure
        if (game_data.getPlayerDistanceFromTreasure() < game_data.getThreshold_distance() )
        {
            game_data.getPlayer().addTreasureFound(game_data.getNext_treasure());
            // TO DO: update Firebase
        }
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


    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {

        List<Feature> symbolLayerIconFeatureList = new ArrayList<>();
        symbolLayerIconFeatureList.add(Feature.fromGeometry(
                Point.fromLngLat(-57.225365, -33.213144)));
        symbolLayerIconFeatureList.add(Feature.fromGeometry(
                Point.fromLngLat(-54.14164, -33.981818)));
        symbolLayerIconFeatureList.add(Feature.fromGeometry(
                Point.fromLngLat(-56.990533, -30.583266)));

        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/mapbox/cjf4m44iw0uza2spb3q0a7s41")

// Add the SymbolLayer icon image to the map style
                .withImage(ICON_ID, BitmapFactory.decodeResource(
                        GameActivity.this.getResources(), R.drawable.treasure))

// Adding a GeoJson source for the SymbolLayer icons.
                .withSource(new GeoJsonSource(SOURCE_ID,
                        FeatureCollection.fromFeatures(symbolLayerIconFeatureList)))

// Adding the actual SymbolLayer to the map style. An offset is added that the bottom of the red
// marker icon gets fixed to the coordinate, rather than the middle of the icon being fixed to
// the coordinate point. This is offset is not always needed and is dependent on the image
// that you use for the SymbolLayer icon.
                .withLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                        .withProperties(PropertyFactory.iconImage(ICON_ID),
                                PropertyFactory.iconSize(0.05f),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true),
                                iconOffset(new Float[] {0f, -9f}))
                ), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

// Map is set up and the style has loaded. Now you can add additional data or make other map adjustments.


            }
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
}




