package ch.epfl.esl.sportstracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MyProfileFragment extends Fragment {

    public static final String USER_PROFILE = "USER_PROFILE";
    public static final String USER_ID = "USER_ID";

    private final String TAG = this.getClass().getSimpleName();

    private static final int EDIT_PROFILE_INFO = 1;

    private OnFragmentInteractionListener mListener;
    private Profile userProfile;
    private View fragmentView;
    private String userID;

    public MyProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_my_profile, container, false);


        Intent intent = getActivity().getIntent();
        userID = intent.getExtras().getString(USER_ID);
        readUserProfile();
        //Commented-out: sendProfileToWatch();

        return fragmentView;
    }

    private void readUserProfile() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference profileRef = database.getReference("profiles");
        profileRef.child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String user_db = dataSnapshot.child("username").getValue(String.class);
                String password_db = dataSnapshot.child("password").getValue(String.class);
                String photo = dataSnapshot.child("photo").getValue(String.class);

                userProfile = new Profile(user_db, password_db);
                userProfile.password = password_db;
                userProfile.photoPath = photo;

                setUserImageAndProfileInfo();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Empty
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_my_profile, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                Intent intentEditProfile = new Intent(getActivity(), EditProfileActivity.class);
                intentEditProfile.putExtra(USER_ID, userID);
                startActivityForResult(intentEditProfile, EDIT_PROFILE_INFO);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PROFILE_INFO && resultCode == AppCompatActivity.RESULT_OK) {
            userProfile = (Profile) data.getSerializableExtra(USER_PROFILE);
            if (userProfile != null) {
                setUserImageAndProfileInfo();
            }
        }
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

    private void setUserImageAndProfileInfo() {
        //  Reference to an image file in Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl
                (userProfile.photoPath);
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                if (isAdded()) {
                    final Bitmap selectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes
                            .length);
                    ImageView imageView = fragmentView.findViewById(R.id.userImage);
                    imageView.setImageBitmap(selectedImage);
                }
            }
        });

        TextView usernameTextView = fragmentView.findViewById(R.id.usernameValue);
        usernameTextView.setText(userProfile.username);

        TextView passwordTextView = fragmentView.findViewById(R.id.passwordValue);
        passwordTextView.setText(userProfile.password);
    }

    private void sendProfileToWatch() {
        Intent intentWear = new Intent(getActivity(), WearService.class);
        intentWear.setAction(WearService.ACTION_SEND.PROFILE_SEND.name());
        intentWear.putExtra(WearService.PROFILE, userProfile);
        getActivity().startService(intentWear);
    }
}
