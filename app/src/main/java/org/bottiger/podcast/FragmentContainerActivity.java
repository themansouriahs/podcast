package org.bottiger.podcast;

import org.bottiger.podcast.Animations.DepthPageTransformer;
import org.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.views.MyCustomViewPager;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.transition.Scene;
import android.transition.TransitionInflater;
import android.view.ViewGroup;
import android.view.ViewStub;


public class FragmentContainerActivity extends DrawerActivity implements
		OnItemSelectedListener {

	@Deprecated
	public static final boolean debugging = ApplicationConfiguration.DEBUGGING;

	protected final PodcastLog log = PodcastLog.getDebugLog(getClass(), 0);

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	public static ViewPager mViewPager;

	private long SubscriptionFeedID = -1;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	protected SectionsPagerAdapter mSectionsPagerAdapter; // FIXME not static
	protected SectionsPagerAdapter mSectionsPagerAdapter2; // FIXME not static

	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;

    private Scene mSceneSubscriptions;
    private Scene mSceneFeed;
	
	private ViewStub mViewStub;
    private MyCustomViewPager mInflatedViewStub;

	private FeedFragment mFeedFragment = null;
    private Playlist mPlaylist;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFragmentManager = getSupportFragmentManager(); // getSupportFragmentManager();

		if (ApplicationConfiguration.DEBUGGING)
			mFragmentManager.enableDebugLogging(true);

		mFragmentTransaction = mFragmentManager.beginTransaction();

		mViewStub = (ViewStub) findViewById(R.id.app_content);

        mPlaylist = new Playlist(this,30);
        mPlaylist.populatePlaylistIfEmpty();

		createViewPager();
        createScenes(mViewPager);
	}
	
	public SectionsPagerAdapter getSectionsPagerAdapter() {
		return this.mSectionsPagerAdapter;
	}
	
	private void createDownloadFragment() {
		DownloadFragment fragment = new DownloadFragment();
		mFragmentManager.beginTransaction().replace(R.id.app_content, fragment).commit();
	}

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void createScenes(ViewGroup viewGroup) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSceneSubscriptions = Scene.getSceneForLayout(viewGroup, R.layout.subscription_list, this);
            mSceneFeed = Scene.getSceneForLayout(viewGroup, R.layout.transition_test, this);
        }
    }
	
	private void createViewPager() {
		mInflatedViewStub = (MyCustomViewPager) mViewStub.inflate();

		mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);
		mViewPager = (ViewPager) mInflatedViewStub.findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(2); // 3
		mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setPageTransformer(true, new DepthPageTransformer());

        if (mPlaylist.isEmpty())
            mViewPager.setCurrentItem(1);
	}

	@Override
	public void onItemSelected(long id) {
        SubscriptionFeedID = id;
        mSectionsPagerAdapter.setSubID(id);


        //mFragmentManager.beginTransaction().replace(R.id.subscription_fragment_container, mSectionsPagerAdapter.getItem(SectionsPagerAdapter.FEED)).commit();
		mSectionsPagerAdapter.notifyDataSetChanged();
        mViewPager.setCurrentItem(2);
	}

	/*
	 * Override BACK button
	 */
	@Override
	public void onBackPressed() {
		//if (mViewPager.getCurrentItem() == SectionsPagerAdapter.SUBSCRIPTION) {
			////mViewPager.setCurrentItem(SectionsPagerAdapter.SUBSCRIPTION, true); // defect
            //mSectionsPagerAdapter.resetPlatlistFragment();
		//} else {
			super.onBackPressed(); // This will pop the Activity from the stack.
		//}
	}


    /**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
		// http://stackoverflow.com/questions/10396321/remove-fragment-page-from-viewpager-in-android/10399127#10399127

		public static final int PLAYLIST = 0;
		public static final int SUBSCRIPTION = 1;
		public static final int FEED = 2;

		private static final int MAX_FRAGMENTS = 3;

        public long subid = -1;

		private Fragment[] mFragments = new Fragment[MAX_FRAGMENTS];
        private ViewGroup mViewGroup;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		// Hack:
		// http://stackoverflow.com/questions/7263291/viewpager-pageradapter-not-updating-the-view/7287121#7287121
		@Override
		public int getItemPosition(Object object) {
			return PagerAdapter.POSITION_NONE;
		}

		@Override
		public Fragment getItem(int i) {

            Fragment fragment = null;
            log.debug("inside: getItem(" + i + ")");

            // No episodes - only show the playlist
            if (getCount() == 1) {
                if (i == 0)
                    fragment = getSubscriptionFragmentContent(subid);
            } else {
                if (i == SUBSCRIPTION) {
                    fragment = getSubscriptionFragmentContent(subid);
                } else if (i == PLAYLIST) {
                    fragment = new PlaylistFragment();
                } else if (i == FEED) {
                    fragment = getFeedFragmentContent();
                }
            }

			mFragments[i] = fragment;
			Bundle args = new Bundle();
			fragment.setArguments(args);
			return fragment;
		}

        public void resetPlatlistFragment() {
            refreshData(SUBSCRIPTION);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);
        }

		@Override
		public int getCount() {
            int fragmentsCount = MAX_FRAGMENTS;

            if (SubscriptionFeedID <= 0)
                fragmentsCount--;

            if (!showPlaylist())
                fragmentsCount--;

            return fragmentsCount;
		}

        private boolean showPlaylist() {
            return true;
        }

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case PLAYLIST:
				return getString(R.string.title_section3).toUpperCase();
			case SUBSCRIPTION:
				return getString(R.string.title_section1).toUpperCase();
			case FEED:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
		}

		public void refreshData(int position) {
			Fragment fragment = mFragments[position];
			switch (position) {
			case PLAYLIST:
				PlaylistFragment recentFragment = (PlaylistFragment) fragment;
				recentFragment.refreshView();
			case SUBSCRIPTION:
				ViewPagerSubscriptionFragment vpsf = (ViewPagerSubscriptionFragment) fragment;
                vpsf.fillContainerWithSubscriptions();
				return;
			case FEED:
				// return getString(R.string.title_section2).toUpperCase();
				return;
			}
		}

        public void setSubID(long subID) {
            this.subid = subID;

        }
    }

	private Fragment getSubscriptionFragmentContent(long id) {
		log.debug("inside: getSubscriptionFragmentContent()");
        /*
        if (id > 0) {
            SubscriptionFeedID = id;
            return getFeedFragmentContent();
        }*/
        ViewPagerSubscriptionFragment viewPagerSubscriptionFragment = new ViewPagerSubscriptionFragment();
        viewPagerSubscriptionFragment.setOnItemSelectedListener(this);
        return viewPagerSubscriptionFragment;
	}

	private Fragment getFeedFragmentContent() {
        Subscription subscription;
        if (SubscriptionFeedID > 0) {
            subscription = Subscription.getById(getContentResolver(), SubscriptionFeedID);
        } else {
            subscription = Subscription.getFirst(getContentResolver());
        }

        mFeedFragment = FeedFragment.newInstance(subscription);
		return mFeedFragment;
	}
}
