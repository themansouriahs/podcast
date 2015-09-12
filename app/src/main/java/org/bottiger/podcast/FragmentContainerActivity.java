package org.bottiger.podcast;

import org.bottiger.podcast.Animations.DepthPageTransformer;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.views.SlidingTab.SlidingTabLayout;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
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

	@Deprecated
	public static final boolean debugging = ApplicationConfiguration.DEBUGGING;

	protected final PodcastLog log = PodcastLog.getDebugLog(getClass(), 0);

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

    private SlidingTabLayout mSlidingTabLayout;

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


        //mViewPager.setOnPageChangeListener(mPageChangeListener);

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                int lockMode = position == 0 ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
                //mDrawerLayout.setDrawerLockMode(lockMode);
            }
        });

        // BEGIN_INCLUDE (tab_colorizer)
        // Set a TabColorizer to customize the indicator and divider colors. Here we just retrieve
        // the tab at the position, and return it's set color
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
                //return getResources().getColor(R.color.colorSecondary);
                return getResources().getColor(R.color.white_opaque);
                //return mTabs.get(position).getIndicatorColor();
            }

            @Override
            public int getDividerColor(int position) {
                return getResources().getColor(R.color.colorSecondary);
                //return mTabs.get(position).getDividerColor();
            }

        });


        mViewPager.setPageTransformer(true, new DepthPageTransformer());

        if (((SoundWaves)getApplication()).IsFirstRun())
            mViewPager.setCurrentItem(1);


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

		public static final int PLAYLIST = 0;
		public static final int SUBSCRIPTION = 1;
		public static final int DISCOVER = 2;

		private static final int MAX_FRAGMENTS = 3;

        private ViewPager mContainer;

		private Fragment[] mFragments = new Fragment[MAX_FRAGMENTS];
        private ViewGroup mViewGroup;

		public SectionsPagerAdapter(FragmentManager fm, ViewPager container) {
			super(fm);
            mContainer = container;
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

    /**
     * This class represents a tab to be displayed by {@link ViewPager} and it's associated
     * {@link SlidingTabLayout}.
     */
    static class SamplePagerItem {
        private final CharSequence mTitle;
        private final int mIndicatorColor;
        private final int mDividerColor;

        SamplePagerItem(CharSequence title, int indicatorColor, int dividerColor) {
            mTitle = title;
            mIndicatorColor = indicatorColor;
            mDividerColor = dividerColor;
        }

        /**
         * @return A new {@link Fragment} to be displayed by a {@link ViewPager}
         */
        Fragment createFragment() {
            return null;//return ContentFragment.newInstance(mTitle, mIndicatorColor, mDividerColor);
        }

        /**
         * @return the title which represents this tab. In this sample this is used directly by
         * {@link android.support.v4.view.PagerAdapter#getPageTitle(int)}
         */
        CharSequence getTitle() {
            return mTitle;
        }

        /**
         * @return the color to be used for indicator on the {@link SlidingTabLayout}
         */
        int getIndicatorColor() {
            return mIndicatorColor;
        }

        /**
         * @return the color to be used for right divider on the {@link SlidingTabLayout}
         */
        int getDividerColor() {
            return mDividerColor;
        }
    }
}
