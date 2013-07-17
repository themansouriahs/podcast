package org.bottiger.podcast;


import org.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PodcastLog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

public class FragmentContainerActivity extends DrawerActivity implements OnItemSelectedListener {
	
	@Deprecated
	public static final boolean debugging = ApplicationConfiguration.DEBUGGING;
	
	protected final PodcastLog log = PodcastLog.getDebugLog(getClass(), 0);
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;

	private long SubscriptionFeedID = 0;

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
	private FragmentTransaction mFragmentTransition;

	private FeedFragment mFeedFragment = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * BugSenseHandler.initAndStartSession(MainActivity.this, ((SoundWaves)
		 * this.getApplication()).getBugSenseAPIKey());
		 */
		// 3132
		// PodcastOpenHelper helper = new PodcastOpenHelper(this);
		// helper.getWritableDatabase().execSQL("update subscriptions set status = "
		// + Subscription.STATUS_SUBSCRIBED);

		mFragmentManager = getSupportFragmentManager(); // getSupportFragmentManager();

		if (debugging)
			mFragmentManager.enableDebugLogging(true);

		mFragmentTransition = mFragmentManager.beginTransaction();
		mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);
		
		ViewStub stub = (ViewStub) findViewById(R.id.app_content);
	    View inflated = stub.inflate();
		
		mViewPager = (ViewPager) inflated; // (ViewPager) inflated.findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);
		// Set up the ViewPager with the sections adapter.
		mViewPager.setAdapter(mSectionsPagerAdapter);

	}
	
	@Override
	public void onItemSelected(long id) {
		SubscriptionFeedID = id;
		mSectionsPagerAdapter.notifyDataSetChanged();
		mViewPager.setCurrentItem(2);
	}
	
	/*
	 * Override BACK button
	 */
	@Override
	public void onBackPressed() {
		if (mViewPager.getCurrentItem() == SectionsPagerAdapter.FEED) {
			mViewPager.setCurrentItem(SectionsPagerAdapter.SUBSRIPTION, true);
		} else {
			super.onBackPressed(); // This will pop the Activity from the stack.
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
		// http://stackoverflow.com/questions/10396321/remove-fragment-page-from-viewpager-in-android/10399127#10399127

		public static final int PLAYLIST = 0;
		public static final int SUBSRIPTION = 1;
		public static final int FEED = 2;

		private static final int MAX_FRAGMENTS = 3;

		private Fragment[] mFragments = new Fragment[MAX_FRAGMENTS];

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
			Fragment fragment;
			log.debug("inside: getItem(" + i + ")");
			if (i == SUBSRIPTION) {
				fragment = getSubscriptionFragmentContent();
			} else if (i == PLAYLIST) {
				fragment = new RecentItemFragment(); // new
														// DummySectionFragment();
			} else if (i == FEED) {
				fragment = getFeedFragmentContent();
			} else {
				fragment = new DummySectionFragment();
				// fragment = new PlayerFragment();
			}
			mFragments[i] = fragment;
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return (SubscriptionFeedID == 0) ? MAX_FRAGMENTS - 1
					: MAX_FRAGMENTS;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case PLAYLIST:
				return getString(R.string.title_section3).toUpperCase();
			case SUBSRIPTION:
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
				RecentItemFragment recentFragment = (RecentItemFragment) fragment;
				recentFragment.refreshView();
			case SUBSRIPTION:
				// return getString(R.string.title_section1).toUpperCase();
				return;
			case FEED:
				// return getString(R.string.title_section2).toUpperCase();
				return;
			}
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		public DummySectionFragment() {
		}

		public static final String ARG_SECTION_NUMBER = "section_number";

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			TextView textView = new TextView(getActivity());
			textView.setGravity(Gravity.CENTER);
			Bundle args = getArguments();
			textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
			return textView;
		}
	}

	private Fragment getSubscriptionFragmentContent() {
		log.debug("inside: getSubscriptionFragmentContent()");
		return new SubscriptionsFragment();
	}

	private Fragment getFeedFragmentContent() {
		Subscription subscription = Subscription.getById(getContentResolver(),
				SubscriptionFeedID);
		mFeedFragment = FeedFragment.newInstance(subscription);

		return mFeedFragment;
	}
}
