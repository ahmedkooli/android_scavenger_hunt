package ch.epfl.esl.sportstracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static ch.epfl.esl.sportstracker.MyProfileFragment.USER_ID;

public class MainActivity extends AppCompatActivity implements NewRecordingFragment
        .OnFragmentInteractionListener, MyProfileFragment.OnFragmentInteractionListener,
        MyHistoryFragment.OnFragmentInteractionListener {

    private final String TAG = this.getClass().getSimpleName();

    private NewRecordingFragment newRecFragment;
    private MyProfileFragment myProfileFragment;
    private MyHistoryFragment myHistoryFragment;
    private SectionsStatePagerAdapter mSectionStatePagerAdapter;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSectionStatePagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());

        // Do this in case of detaching of Fragments
        myProfileFragment = new MyProfileFragment();
        newRecFragment = new NewRecordingFragment();
        myHistoryFragment = new MyHistoryFragment();

        ViewPager mViewPager = findViewById(R.id.mainViewPager);
        setUpViewPager(mViewPager);

        // Set NewRecordingFragment as default tab once started the activity
        mViewPager.setCurrentItem(mSectionStatePagerAdapter.getPositionByTitle(getString(R.string
                .tab_title_new_recording)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "HAHAHHAHAHAHAHAHAHHA");

    }

    private void setUpViewPager(ViewPager mViewPager) {
        mSectionStatePagerAdapter.addFragment(myProfileFragment, getString(R.string
                .tab_title_my_profile));
        mSectionStatePagerAdapter.addFragment(newRecFragment, getString(R.string
                .tab_title_new_recording));
        mSectionStatePagerAdapter.addFragment(myHistoryFragment, getString(R.string
                .tab_title_history));
        mViewPager.setAdapter(mSectionStatePagerAdapter);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
