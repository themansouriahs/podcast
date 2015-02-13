package org.bottiger.podcast;

import org.bottiger.podcast.Animations.DepthPageTransformer;
import org.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.views.MyCustomViewPager;
import org.bottiger.podcast.views.SlidingTab.SlidingTabLayout;

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
import android.util.Log;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


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
	protected SectionsPagerAdapter mSectionsPagerAdapter;

	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;

    private SlidingTabLayout mSlidingTabLayout;
    private List<SamplePagerItem> mTabs = new ArrayList<SamplePagerItem>();

    private Scene mSceneSubscriptions;
    private Scene mSceneFeed;

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

		mInflatedViewStub = (MyCustomViewPager) findViewById(R.id.app_content);

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
        }
    }
	
	private void createViewPager() {
		mViewPager = mInflatedViewStub;
        mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager, mViewPager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(1);
		mViewPager.setAdapter(mSectionsPagerAdapter);

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);

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

        if (mPlaylist.isEmpty())
            mViewPager.setCurrentItem(1);
	}

    private boolean first = true;
	@Override
	public void onItemSelected(long id) {
        SubscriptionFeedID = id;
        mSectionsPagerAdapter.setSubID(id);

        Fragment f = FeedFragment.newInstance(Subscription.getById(getContentResolver(), id));
        //mFragmentManager.beginTransaction().add(R.id.app_content, f).commit();
        //mFragmentManager.beginTransaction().replace(R.id.app_content, f).commit();
        mSectionsPagerAdapter.replace(1, f);
        //mSectionsPagerAdapter.notifyDataSetChanged();

        //mFragmentManager.executePendingTransactions();

        //mSectionsPagerAdapter.setSubID(id);
	}

	/*
	 * Override BACK button
	 */
	@Override
	public void onBackPressed() {
        boolean isHandled = mSectionsPagerAdapter.back();

        if (!isHandled)
            super.onBackPressed(); // This will pop the Activity from the stack.
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

		private static final int MAX_FRAGMENTS = 2;

        public long subid = -1;

        private ViewPager mContainer;

		private Fragment[] mFragments = new Fragment[MAX_FRAGMENTS];
        private ViewGroup mViewGroup;

        /**
         * List of page fragments to return to in onBack();
         */
        private List<Fragment> mBackFragments;

		public SectionsPagerAdapter(FragmentManager fm, ViewPager container) {
			super(fm);
            mContainer = container;
		}

        /**
         * Replaces the view pager fragment at specified position.
         */
        public void replace(int position, Fragment fragment) {
            // Get currently active fragment.
            Fragment old_fragment = mFragments[position];
            if (old_fragment == null) {
                return;
            }

            Log.d("Frag.GetI", "replace - start");
            // Replace the fragment using transaction and in underlaying array list.
            // NOTE .addToBackStack(null) doesn't work
            this.startUpdate(mContainer);
            //mFragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            //        .remove(old_fragment).add(mContainer.getId(), fragment)
            //        .commit();
            mFragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(mContainer.getId(), fragment)
                    .commit();

            mFragments[position] = fragment;
            nnew = fragment;
            Log.d("Frag.GetI", "replace - end");
            this.notifyDataSetChanged();
            this.finishUpdate(mContainer);
        }
        Fragment nnew = null;

        /**
         * Replaces the fragment at specified position and stores the current fragment to back stack
         * so it can be restored by #back().
         */
        public void start(int position, Fragment fragment) {
            // Remember current fragment.
            mBackFragments.set(position, mFragments[position]);

            // Replace the displayed fragment.
            this.replace(position, fragment);
        }

        /**
         * Replaces the current fragment by fragment stored in back stack. Does nothing and returns
         * false if no fragment is back-stacked.
         */
        public boolean back() {
            int position = mContainer.getCurrentItem();
            Fragment fragment = mFragments[position];
            if (fragment == null) {
                // Nothing to go back.
                return false;
            }

            if (fragment instanceof BackButtonListener) {
                BackButtonListener bbl = ((BackButtonListener)fragment);
                boolean canGoBack = bbl.canGoBack();
                if (canGoBack) {
                    bbl.back();
                    return true;
                }
            }

            return false;
        }

        // Get rid of this
        private boolean showingFeed() {
            int position = mContainer.getCurrentItem();

            Fragment fragment = mFragments[position];
            if (fragment == null) {
                // Nothing to go back.
                return false;
            }

            if (fragment instanceof BackButtonListener) {
                BackButtonListener bbl = ((BackButtonListener)fragment);
                boolean canGoBack = bbl.canGoBack();
                if (canGoBack) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
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

            Fragment fragment = null; //mFragments[position];
            String fs = fragment == null ? "none" : fragment.getTag();
            Log.d("Frag.GetI", "inside: getItem(" + position + "). fragment: " + fs);


            if (position == SUBSCRIPTION) {
                fragment = new ViewPagerSubscriptionFragment();
                        /*
                        if (nnew != null) {
                            fragment = getFeedFragmentContent();
                        } else {
                            fragment = getSubscriptionFragmentContent(subid);
                        }*/
            } else if (position == PLAYLIST) {
                fragment = new PlaylistFragment();
            }

            mFragments[position] = fragment;
			Bundle args = new Bundle();
			fragment.setArguments(args);
			return fragment;
		}



        @Override
        public void startUpdate(ViewGroup container) {
            makeToolbarTransparent(showingFeed());
            super.startUpdate(container);
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
			case FEED:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
		}

		public void refreshData(int position) {
		}

        public void setSubID(long subID) {
            long oldId = subid;
            this.subid = subID;

            if (oldId == -1) {
                //notifyDataSetChanged();
                refreshData(FEED);
            } else {
                refreshData(FEED);
            }
        }
    }

	private Fragment getSubscriptionFragmentContent(long id) {
		log.debug("inside: getSubscriptionFragmentContent()");

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

        if (mFeedFragment == null)
            mFeedFragment = FeedFragment.newInstance(subscription);
        else
            mFeedFragment.bindNewSubscrption(subscription, false);

        makeToolbarTransparent(true);
		return mFeedFragment;
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
