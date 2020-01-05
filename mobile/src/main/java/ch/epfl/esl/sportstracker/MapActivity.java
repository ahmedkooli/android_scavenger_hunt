package ch.epfl.esl.sportstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapActivity extends AppCompatActivity {

    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        userID = getIntent().getStringExtra("USER_ID");

        ImageButton buttonParc = findViewById(R.id.parcButton);
        buttonParc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMapNameAndImage(MAP.PARC);
            }
        });

        ImageButton buttonRlc = findViewById(R.id.rlcButton);
        buttonRlc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMapNameAndImage(MAP.RLC);
            }
        });
    }

    private void setMapNameAndImage(MAP map) {
        Drawable image = getResources().getDrawable(R.drawable.ic_logo);
        String name = "Select Map";

        ImageView mapImage = findViewById(R.id.imageMap);
        TextView mapName = findViewById(R.id.nameMap);

        switch (map) {
            case PARC:
                image = getResources().getDrawable(R.drawable.parc_scient);
                name = "Parc Scientifique";
                break;
            case RLC:
                image = getResources().getDrawable(R.drawable.rlc);
                name = "Rolex Learning Center";
                break;
        }

        mapName.setText(name);
        mapImage.setImageDrawable(image);
    }

    public void onStartGame(View view) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("profiles");
        ref.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        TextView mapName = findViewById(R.id.nameMap);

                        // Map not selected
                        if(mapName.getText().toString().equals("Select Map")){
                            Toast.makeText(MapActivity.this,
                                    "Please choose a map",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Map selected
                        else {
                            // Set chosen map
                            ref.child(userID).child("map").setValue(mapName.getText().toString());

                            // Set opponent's map
                            String opponent =  dataSnapshot.child(userID).
                                    child("matched player").getValue(String.class).
                                    replace("Choosing ", "");

                            String keyOpponent = getKey(dataSnapshot,
                                    "username", opponent);

                            ref.child(keyOpponent).child("map").setValue(mapName.getText().toString());
                        }

                        String mapSelected = dataSnapshot.child(userID).child("map").getValue(String.class);

                        if (mapSelected.equals("Parc Scientifique") ||
                                mapSelected.equals("Rolex Learning Center")){
                            Log.w("ds","equals");

                            Intent intentStartGame = new Intent(MapActivity.this, GameActivity.class);
                            Bundle extras = new Bundle();
                            extras.putString("USER_ID",userID);
                            extras.putString("MAP",mapSelected);
                            intentStartGame.putExtras(extras);
                            extras = intentStartGame.getExtras();
                            Log.v("avanr", String.valueOf(extras));
                            userID = extras.getString("USER_ID");
                            Log.v("avanr userid", userID);
                            ref.removeEventListener(this);
                            startActivity(intentStartGame);
                            finish();
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private String getKey(DataSnapshot ds, String parameter, String value){

        String key = "";

        for (DataSnapshot chld: ds.getChildren()) {
            if (chld.child(parameter).getValue().equals(value)){
                key = chld.getKey();
            }
        }

        return key;
    }

    enum MAP {PARC, RLC}
}
