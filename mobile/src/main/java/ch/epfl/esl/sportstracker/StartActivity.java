package ch.epfl.esl.sportstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ch.epfl.esl.sportstracker.MyProfileFragment.USER_ID;

public class StartActivity extends AppCompatActivity {

    private ArrayList<String> data = new ArrayList<>();
    private String userID;
    private ValueEventListener UserStatusListener;
    private ChildEventListener NewUserListener;
    private DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("profiles");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        userID = getIntent().getStringExtra("USER_ID");

        setContentView(R.layout.activity_start);
        ListView lv = findViewById(R.id.listview);

        generateListContent();

        final ArrayAdapter<String> adapter = new MyListAdaper(this, R.layout.list_item, data);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });


         NewUserListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String newuserID = dataSnapshot.getKey();
                if (!(userID.equals(newuserID))){
                    Map<String, Object> info = (Map<String, Object>) dataSnapshot.getValue();
                    String username = (String) info.get("username");
                    Boolean game_status = (Boolean) info.get("game status");
                    if (!game_status) {
                        if (data.contains(username)) {
                            data.remove(username);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        if (!(data.contains(username)) && !(userID.equals(newuserID))) {
                            data.add(username);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String newuserID = dataSnapshot.getKey();
                if (!(userID.equals(newuserID))){
                    Map<String, Object> info = (Map<String, Object>) dataSnapshot.getValue();
                    String username = (String) info.get("username");
                    Boolean game_status = (Boolean) info.get("game status");
                    if (!game_status) {
                        if (data.contains(username)) {
                            data.remove(username);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        if (!data.contains(username) && userID != newuserID) {
                            data.add(username);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Map<String, Object> info = (Map<String, Object>) dataSnapshot.getValue();
                String username = (String) info.get("username");
                data.remove(username);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        ref.addChildEventListener(NewUserListener);
    }

    private void generateListContent() {

         UserStatusListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        if (userID != null) {
                            //Get map of users in datasnapshot
                            final String userActive = dataSnapshot.child(userID).child("username").getValue(String.class);
                            collectUsers((Map<String, Object>) dataSnapshot.getValue(), userActive);

                            final String userMatched = dataSnapshot.child(userID).child("matched player").getValue(String.class);

                            if (userMatched.contains("None")) {
                                // Handle no request
                            }

                            if (userMatched.contains("Request")) {
                                // Handle received request

                                // Build an AlertDialog
                                AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);

                                // Set a title for alert dialog
                                builder.setTitle("You received a game request.");

                                // Ask the final question
                                builder.setMessage("Do you want to play?");

                                // Set click listener for alert dialog buttons
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                // User clicked the Yes button

                                                // Matched player for requested user
                                                final String keyUserMatched = getKey(dataSnapshot,
                                                        "username",
                                                        userMatched.replace("Request ", ""));

                                                ref.child(keyUserMatched).child("matched player")
                                                        .setValue("Choosing " + userActive);


                                                // Matched player for active user
                                                ref.child(userID).child("matched player")
                                                        .setValue("Waiting " +
                                                                userMatched.replace("Request ", ""));
                                                break;

                                            case DialogInterface.BUTTON_NEGATIVE:
                                                // User clicked the No button

                                                // Matched player for requested user
                                                final String keyUserMatched2 = getKey(dataSnapshot,
                                                        "username",
                                                        userMatched.replace("Request ", ""));

                                                ref.child(keyUserMatched2).child("matched player")
                                                        .setValue("None");


                                                // Matched player for active user
                                                ref.child(userID).child("matched player")
                                                        .setValue("None");
                                                break;
                                        }
                                    }
                                };

                                // Set the alert dialog yes button click listener
                                builder.setPositiveButton("Yes", dialogClickListener);

                                // Set the alert dialog no button click listener
                                builder.setNegativeButton("No", dialogClickListener);

                                AlertDialog dialog = builder.create();
                                // Display the alert dialog on interface
                                dialog.show();

                            }

                            if (userMatched.contains("Waiting")) {
                                // Handle waiting for map
                                ref.child(userID).child("game status").setValue(false);

                                // Start WaitForMapActivity
                                Intent intentMapWait = new Intent(StartActivity.this, WaitForMapActivity.class);
                                intentMapWait.putExtra("USER_ID", userID);
                                ref.removeEventListener(UserStatusListener);
                                ref.removeEventListener(NewUserListener);
                                startActivity(intentMapWait);
                                finish();
                            }

                            if (userMatched.contains("Choosing")) {
                                // Handle choosing map
                                ref.child(userID).child("game status").setValue(false);
                                //ref.child(userID).child("matched player").
                                //        setValue(userMatched.replace("Choosing ",""));

                                // Start MapActivity
                                Intent intentMapChoice = new Intent(StartActivity.this, MapActivity.class);
                                intentMapChoice.putExtra("USER_ID", userID);
                                ref.removeEventListener(UserStatusListener);
                                ref.removeEventListener(NewUserListener);
                                startActivity(intentMapChoice);
                                finish();
                            }

                            if (userMatched.contains("Declined")) {
                                // Handle request declined
                            }

                            if (userMatched.contains("Sent")) {
                                // Handle sent request
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                };
        ref.addValueEventListener(UserStatusListener);

    }


    private void collectUsers(Map<String, Object> users, String userActive) {
        ArrayList<String> activeUsers = new ArrayList<>();

        //iterate through each user, ignoring their UID
        for (Map.Entry<String, Object> entry : users.entrySet()){

            //Get user map
            Map singleUser = (Map) entry.getValue();

            if ((Boolean) singleUser.get("game status") && (singleUser.get("username") != userActive)){
                activeUsers.add((String) singleUser.get("username"));
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MyListAdaper extends ArrayAdapter<String> {
        private int layout;
        private List<String> mObjects;
        private MyListAdaper(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            mObjects = objects;
            layout = resource;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder mainViewholder = null;
            if(convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.list_item_thumbnail);
                viewHolder.title = (TextView) convertView.findViewById(R.id.list_item_text);
                viewHolder.button = (Button) convertView.findViewById(R.id.list_item_btn);
                convertView.setTag(viewHolder);
            }

            mainViewholder = (ViewHolder) convertView.getTag();
            mainViewholder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // A modifier pour faire seulement quand on a pas request
                    if (true) {
                        Toast.makeText(getContext(),
                                "Button was clicked for list item "
                                        + data.get(position), Toast.LENGTH_SHORT).show();

                        // Build an AlertDialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);

                        // Set a title for alert dialog
                        builder.setTitle("Play request.");

                        // Ask the final question
                        builder.setMessage("Do you want to send a game request?");

                        // Set click listener for alert dialog buttons
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        // User clicked the Yes button

                                        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("profiles");
                                        ref.addListenerForSingleValueEvent(
                                                new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                                        // Matched player for requested user
                                                        String keyUserMatched = getKey(dataSnapshot,
                                                                "username", data.get(position));

                                                        ref.child(keyUserMatched).child("matched player")
                                                                .setValue("Request " +
                                                                        dataSnapshot.child(userID).
                                                                                child("username").getValue());

                                                        // Matched player for active user
                                                        ref.child(userID).child("matched player")
                                                                .setValue("Sent " + data.get(position));
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        //handle databaseError
                                                    }
                                                });
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        // User clicked the No button
                                        // Nothing to do
                                        break;
                                }
                            }
                        };

                        // Set the alert dialog yes button click listener
                        builder.setPositiveButton("Yes", dialogClickListener);

                        // Set the alert dialog no button click listener
                        builder.setNegativeButton("No",dialogClickListener);

                        AlertDialog dialog = builder.create();
                        // Display the alert dialog on interface
                        dialog.show();

                    }

                    else{
                        Toast.makeText(getContext(),
                                "You already have a pending request! Please wait.", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            mainViewholder.title.setText(getItem(position));

            return convertView;
        }
    }


    public class ViewHolder {

        ImageView thumbnail;
        TextView title;
        Button button;
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Too scared?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (arg0, arg1) -> {
                    Intent intentStartGame = new Intent(StartActivity.this, MainActivity.class);
                    intentStartGame.putExtra("USER_ID", userID);
                    intentStartGame.putExtra("USER_ID", userID);

                    Intent intent = getIntent();
                    userID = intent.getExtras().getString(USER_ID);

                    // Initialize Firebase
                    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("profiles/"+userID);
                    ref.addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot dataSnapshot) {
                                    ref.child("matched player").setValue("None");
                                    ref.child("map").setValue("None");
                                    ref.child("game status").setValue(false);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                    startActivity(intentStartGame);
                    finish();
                }).create().show();
    }




    // deal with the home button
    @Override
    protected void onPause() {
        super.onPause();
        if (!this.isFinishing()) {
            Intent intent = getIntent();
            userID = intent.getExtras().getString(USER_ID);

            // Initialize Firebase
            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("profiles/"+userID);
            ref.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot dataSnapshot) {
                            ref.child("matched player").setValue("None");
                            ref.child("map").setValue("None");
                            ref.child("game status").setValue(false);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
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
}