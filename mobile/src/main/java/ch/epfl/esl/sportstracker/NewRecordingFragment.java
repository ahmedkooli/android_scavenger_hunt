package ch.epfl.esl.sportstracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import static ch.epfl.esl.sportstracker.MyProfileFragment.USER_ID;

public class NewRecordingFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();
    private View fragmentView;

    private static final int START_ACT = 1;

    private OnFragmentInteractionListener mListener;
    private boolean ready = false;
    private String userID;

    public NewRecordingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_new_recording, container, false);

        ready = false;

        ImageButton buttonStart = fragmentView.findViewById(R.id.startButton);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ready = true;
                recordStatusFirebase(ready);
            }
        });

        return fragmentView;
    }

    private void recordStatusFirebase(final boolean status) {

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference profileGetRef = database.getReference("profiles");
        final DatabaseReference statusRef = profileGetRef.child(userID).child("game status");

        statusRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                mutableData.setValue(status);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b,
                                   @Nullable DataSnapshot dataSnapshot) {

                setActivityNameAndImage(status);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getActivity().getIntent();
        userID = intent.getExtras().getString(USER_ID);

        // Initialize Firebase
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("profiles");
        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        ref.child(userID).child("matched player").setValue("None");
                        ref.child(userID).child("map").setValue("None");
                        ref.child(userID).child("game status").setValue(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement " +
                    "OnFragmentInteractionListener");
        }
    }

    private void setActivityNameAndImage(boolean status) {
        ready = status;

        Intent intentStartGame = new Intent(getActivity(), StartActivity.class);
        TextView activityName = fragmentView.findViewById(R.id.nameActivity);

        String name = "";
        if (status == true) {
            name = getString(R.string.start_string);
            activityName.setText(name);

            Toast.makeText(getContext(), "Looking for players!", Toast
                    .LENGTH_SHORT).show();

            intentStartGame.putExtra("USER_ID", userID);
            startActivity(intentStartGame);
        }
        else if (status == false) {
            name = "Waiting to start.";
            activityName.setText(name);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w("finish","ing");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.w("bl","babaoub");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating
     * .html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
