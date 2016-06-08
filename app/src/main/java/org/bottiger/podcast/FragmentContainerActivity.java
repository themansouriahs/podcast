package org.bottiger.podcast;

import org.bottiger.podcast.Animations.DepthPageTransformer;
import org.bottiger.podcast.Animations.ZoomOutPageTransformer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.SlidingTab.SlidingTabLayout;
import org.bottiger.podcast.views.TopPlayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ViewGroup;


public class FragmentContainerActivity extends DrawerActivity {

    public static final int PLAYLIST = 0;
    public static final int SUBSCRIPTION = 1;
    public static final int DISCOVER = 2;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	public ViewPager mViewPager;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	protected SectionsPagerAdapter mSectionsPagerAdapter;

	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;

    private ViewPager mInflatedViewStub;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFragmentManager = getSupportFragmentManager(); // getSupportFragmentManager();

		if (ApplicationConfiguration.DEBUGGING)
			mFragmentManager.enableDebugLogging(true);

		mFragmentTransaction = mFragmentManager.beginTransaction();

		mInflatedViewStub = (ViewPager) findViewById(R.id.app_content);

		// ViewPager setup
        mViewPager = mInflatedViewStub;
        mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager, mViewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3); // 3


        mPagerTaps.addTab(mPagerTaps.newTab().setText("Tab 1"));
        mPagerTaps.addTab(mPagerTaps.newTab().setText("Tab 2"));
        mPagerTaps.addTab(mPagerTaps.newTab().setText("Tab 3"));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mPagerTaps));
        mPagerTaps.setupWithViewPager(mViewPager);

        if (((SoundWaves)getApplication()).IsFirstRun())
            mViewPager.setCurrentItem(DISCOVER);

        createScenes(mViewPager);
	}

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void createScenes(ViewGroup viewGroup) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //mSceneSubscriptions = Scene.getSceneForLayout(viewGroup, R.layout.subscription_list, this);
        }
    }

    /**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		// http://stackoverflow.com/questions/10396321/remove-fragment-page-from-viewpager-in-android/10399127#10399127

		private static final int MAX_FRAGMENTS = 3;

		private Fragment[] mFragments = new Fragment[MAX_FRAGMENTS];

		public SectionsPagerAdapter(FragmentManager fm, ViewPager container) {
			super(fm);
		}

		// Hack:
		// http://stackoverflow.com/questions/7263291/viewpager-pageradapter-not-updating-the-view/7287121#7287121
		@Override
		public int getItemPosition(Object item /* a Fragment */) {
            if (item instanceof PlaylistFragment)
                return PagerAdapter.POSITION_UNCHANGED;

			return PagerAdapter.POSITION_NONE;
		}

		@Override
		public Fragment getItem(int position) {

            Fragment fragment = mFragments[position];
            String fs = fragment == null ? "none" : fragment.getTag();
            Log.d("Frag.GetI", "inside: getItem(" + position + "). fragment: " + fs);


            if (fragment == null) {

                if (position == SUBSCRIPTION) {
                    fragment = new SubscriptionsFragment();
                    //fragment = new DummyFragment();
                } else if (position == PLAYLIST) {
                    fragment = new PlaylistFragment();
                    //fragment = new DummyFragment();
                } else if (position == DISCOVER) {
                    fragment = new DiscoveryFragment();
                    //fragment = new DummyFragment();
                }


                if (fragment == null)
                    fragment = new DummyFragment();

                Bundle args = new Bundle();
                fragment.setArguments(args);
            }

            mFragments[position] = fragment;
			return fragment;
		}

        @Override
        public void setPrimaryItem (ViewGroup container, int position, Object object) {
            Activity activity = this.getItem(position).getActivity();
            boolean HasTinted = false;
            if (activity != null) {
                if (HasTinted) {
                    mViewPager.bringToFront();
                } else {
                    UIUtils.resetStatusBar(activity);
                    mViewPager.bringToFront();
                }
            }
            super.setPrimaryItem(container, position, object);
        }

		@Override
		public int getCount() {
            return MAX_FRAGMENTS;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case PLAYLIST:
				return getString(R.string.title_section3).toUpperCase();
			case SUBSCRIPTION:
				return getString(R.string.title_section1).toUpperCase();
			case DISCOVER:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
		}

    }
}
