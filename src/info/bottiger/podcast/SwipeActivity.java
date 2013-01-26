package info.bottiger.podcast;

import info.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import info.bottiger.podcast.cloud.CloudProvider;
import info.bottiger.podcast.cloud.GoogleReader;
import info.bottiger.podcast.debug.SqliteCopy;
import info.bottiger.podcast.receiver.HeadsetReceiver;
import info.bottiger.podcast.service.PlayerService;
import info.bottiger.podcast.service.PodcastService;
import info.bottiger.podcast.utils.AddPodcastDialog;
import info.bottiger.podcast.utils.Log;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class SwipeActivity extends SlidingFragmentActivity implements
		OnItemSelectedListener {

	public static PodcastService mPodcastServiceBinder = null;
	static boolean mBound = false;
	
	// public static GoogleReader gReader = null;
	public static CloudProvider gReader = null;

	protected static Cursor mCursor = null;
	protected boolean mInit = false;
	protected final Log log = Log.getLog(getClass());
	protected static ComponentName mService = null;

	public static Account mAccount;

	private boolean debugging = true;

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
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private long SubscriptionFeedID = 0;
	
	private AudioManager mAudioManager;
    private ComponentName mRemoteControlResponder;
    
    private SharedPreferences prefs;

    /*
	protected static ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mPodcastServiceBinder = ((PodcastService.PodcastBinder) service)
					.getService();
			//mPodcastServiceBinder.start_update();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mPodcastServiceBinder = null;
			mBound = false;
		}
	};
	*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// BugSenseHandler.initAndStartSession(SwipeActivity.this, "75981add");
		BugSenseHandler.initAndStartSession(SwipeActivity.this,
				((SoundWaves) this.getApplication()).getBugSenseAPIKey());
		if (debugging) {
			Debug.startMethodTracing("calc");

			try {
				SqliteCopy.backupDatabase();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//startService(new Intent(this, PodcastService.class));
		startService(new Intent(this, PlayerService.class));

		setContentView(R.layout.activity_swipe);

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(),
                HeadsetReceiver.class.getName());
        
        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        HeadsetReceiver receiver = new HeadsetReceiver();
        registerReceiver( receiver, receiverFilter );
        //mAudioManager.registerMediaButtonEventReceiver(mRemoteControlResponder);
        
        
        
        //Alarm
        //AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //Intent intent = new Intent(this,PodcastUpdateReceiver.class); 
        //PendingIntent pendingIntent = PendingIntent.getBroadcast(SetReminder.this, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
		
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mFragmentManager = getSupportFragmentManager();
		mFragmentTransition = mFragmentManager.beginTransaction();
		mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);
		
		
		
		//PodcastUpdateManager.setUpdate(this);

		
		
		// set the Behind View
		setBehindContentView(R.layout.download);
		
		
		SlidingMenu menu = getSlidingMenu();

	    menu.setMode(SlidingMenu.LEFT);
	    menu.setShadowWidthRes(R.dimen.shadow_width);
	    //menu.setShadowDrawable(R.drawable.shadow);
	    menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
	    menu.setFadeDegree(0.35f);
	    menu.setTouchModeAbove(SlidingMenu.LEFT);
	    setSlidingActionBarEnabled(true);
		
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		try {
			Account[] a = AccountManager.get(getApplicationContext())
					.getAccountsByType("com.google");
			SwipeActivity.mAccount = a[0];
			gReader = new GoogleReader(SwipeActivity.this, mAccount,
					((SoundWaves) this.getApplication())
							.getGoogleReaderConsumerKey());
			if (a.length > 0) {
				// gReader.refreshAuthToken();
				gReader.getSubscriptionsFromReader();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	protected void onActivityCreated(Bundle savedInstanceState) {

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (debugging)
			Debug.stopMethodTracing();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		BugSenseHandler.closeSession(SwipeActivity.this);
	}

	@Override
	public void onItemSelected(long id) {
		SubscriptionFeedID = id;
		mSectionsPagerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mInit) {
			mInit = false;

			if (mCursor != null)
				mCursor.close();

			//unbindService(serviceConnection);

			// startInit(); FIXME

		}
		
		PodcastService.setAlarm(this);
		/*
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    int minutes = prefs.getInt("interval", 1);
	    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
	    Intent intent = new Intent(this, PodcastService.class);
	    PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
	    am.cancel(pi);
	    // by my own convention, minutes <= 0 means notifications are disabled
	    if (minutes > 0) {
	        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
	            SystemClock.elapsedRealtime() + minutes*60*1000,
	            minutes*60*1000, pi);
	    }
	    */

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mInit = true;

		log.debug("onLowMemory()");
		// finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_swipe, menu);
		return true;
	}

	/**
	 * Right corner menu options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:
			AddPodcastDialog.addPodcast(this);
			return true;
		case R.id.menu_settings:
			Intent i = new Intent(getBaseContext(), SettingsActivity.class);
			startActivity(i);
			return true;
		case R.id.menu_refresh:
			if (mBound)
				mPodcastServiceBinder.start_update();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
		// http://stackoverflow.com/questions/10396321/remove-fragment-page-from-viewpager-in-android/10399127#10399127

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
			if (i == 1) {
				fragment = getSubscriptionFragmentContent();
			} else if (i == 0) {
				fragment = new RecentItemFragment();
			} else {
				fragment = new DummySectionFragment();
				// fragment = new PlayerFragment();
			}
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_section3).toUpperCase();
			case 1:
				return getString(R.string.title_section1).toUpperCase();
			case 2:
				return getString(R.string.title_section2).toUpperCase();
			}
			return null;
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

	/*
	 * Override BACK button
	 * 
	 * @see android.support.v4.app.FragmentActivity#onKeyDown(int,
	 * android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onItemSelected(0);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private Fragment getSubscriptionFragmentContent() {
		if (SubscriptionFeedID == 0) {
			return new SubscriptionsFragment();
		} else {
			Bundle bundle = new Bundle();
			bundle.putLong("subID", SubscriptionFeedID);
			bundle.putInt("ii", 55);
			bundle.putString("hej", "med dig");
			FeedFragment.newInstance(SubscriptionFeedID);
			FeedFragment feed = new FeedFragment();
			// feed.setArguments(bundle);
			// feed.newInstance(SubscriptionFeedID);
			return feed;
		}
	}
}
