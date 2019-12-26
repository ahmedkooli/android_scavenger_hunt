package ch.epfl.esl.sportstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaitForMapActivity extends AppCompatActivity {

    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_map);

        userID = getIntent().getStringExtra("USER_ID");

        final ProgressDialog nDialog;
        nDialog = new ProgressDialog(WaitForMapActivity.this);
        nDialog.setMessage("Waiting for map selection..");
        nDialog.setTitle("Get Map");
        nDialog.setIndeterminate(false);
        nDialog.setCancelable(true);
        nDialog.show();

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("profiles");
        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String map = dataSnapshot.child(userID).child("map").getValue(String.class);

                        if (!map.equals("None")) {
                            // Start GameActivity
                            nDialog.dismiss();

                            Intent intentStartGame = new Intent(WaitForMapActivity.this, GameActivity
                                    .class);
                            startActivity(intentStartGame);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
