package ch.epfl.esl.sportstracker;

import android.net.Uri;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements NewRecordingFragment
        .OnFragmentInteractionListener, MyProfileFragment.OnFragmentInteractionListener,
        MyHistoryFragment.OnFragmentInteractionListener {

    private final String TAG = this.getClass().getSimpleName();

    private NewRecordingFragment newRecFragment;
    private MyProfileFragment myProfileFragment;
    private MyHistoryFragment myHistoryFragment;
    private SectionsStatePagerAdapter mSectionStatePagerAdapter;

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
