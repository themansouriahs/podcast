package org.bottiger.podcast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import org.bottiger.podcast.cloud.CloudProvider;
import org.bottiger.podcast.cloud.GoogleReader;
import org.bottiger.podcast.cloud.drive.DriveSyncer;
import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.images.ImageCacheManager;
import org.bottiger.podcast.images.ImageCacheManager.CacheType;
import org.bottiger.podcast.images.RequestManager;
import org.bottiger.podcast.provider.PodcastProvider;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.HTTPDService;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.service.PodcastService;
import org.bottiger.podcast.utils.AddPodcastDialog;
import org.bottiger.podcast.utils.ControlButtons;
import org.bottiger.podcast.utils.DriveUtils;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.ThemeHelper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap.CompressFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

// Sliding
public class MainActivity extends FragmentActivity implements
		OnItemSelectedListener {

	private static final String ACCOUNT_KEY = "account";

	public static final boolean SHOW_DRAWER = false;
	public static final boolean SHOW_PULL_TO_REFRESH = false;

	private Drive mDriveService = null;
	private final int REQUEST_AUTHORIZATION = 1;
	private final int REQUEST_ACCOUNT_PICKER = 0;
	private static GoogleAccountCredential mCredential;

	public static PodcastService mPodcastServiceBinder = null;
	public static HTTPDService mHTTPDServiceBinder = null;

	static boolean mBound = false;

	// public static GoogleReader gReader = null;
	public static CloudProvider gReader = null;

	protected static Cursor mCursor = null;
	protected boolean mInit = false;
	protected final PodcastLog log = PodcastLog.getDebugLog(getClass(), 0);
	protected static ComponentName mService = null;

	public static Account mAccount;

	public static final boolean debugging = false;

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
	private ViewPager mViewPager;

	private long SubscriptionFeedID = 0;

	private AudioManager mAudioManager;
	private ComponentName mRemoteControlResponder;

	private SharedPreferences prefs;

	private int currentTheme;

	private DriveUtils mDriveUtils;

	/** Painless networking with Volley */
	private static int DISK_IMAGECACHE_SIZE = 1024 * 1024 * 10;
	private static CompressFormat DISK_IMAGECACHE_COMPRESS_FORMAT = CompressFormat.PNG;
	private static int DISK_IMAGECACHE_QUALITY = 100; // PNG is lossless so
														// quality is ignored
														// but must be provided

	public static ServiceConnection mHTTPDServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mHTTPDServiceBinder = ((HTTPDService.HTTPDBinder) service)
					.getService();
			// String out = mHTTPDServiceBinder.getTest();
			// android.util.Log.d("nanoHTTPD", out);

			// init webserver

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mHTTPDServiceBinder = null;
		}
	};

	private String[] mPlanetTitles;
	private LinearLayout mDrawerList;

	private HeadsetReceiver receiver;

	/** Navigation Drawer */
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(MainActivity.this,
				((SoundWaves) this.getApplication()).getBugSenseAPIKey());
		
		setContentView(R.layout.activity_swipe);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		//View pager2 = (ViewPager) findViewById(R.id.drawer_layout);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (debugging) {
			ViewServer.get(this).addWindow(this);
			// Tracing is buggy on emulator
			// Debug.startMethodTracing("calc");

			if (true) {
				try {
					SqliteCopy.backupDatabase();
				} catch (IOException e) { // TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		if (prefs.getBoolean(SettingsActivity.CLOUD_SUPPORT, true)) {

			mCredential = GoogleAccountCredential.usingOAuth2(this,
					DriveSyncer.getScope()); // "https://www.googleapis.com/auth/drive.appdata");
																		// //DriveScopes.DRIVE);
			// mCredential = GoogleAccountCredential.usingOAuth2(this,
			// DriveScopes.DRIVE_FILE); //DriveScopes.DRIVE);
			if (!prefs.contains(ACCOUNT_KEY))
				startActivityForResult(mCredential.newChooseAccountIntent(),
						REQUEST_ACCOUNT_PICKER);
			else
				mCredential.setSelectedAccountName(prefs.getString(ACCOUNT_KEY,
						""));

			new Thread(new Runnable() {
				public void run() {
					try {
						mCredential.getToken();
					} catch (Exception e) {
						e.printStackTrace();
						if (e instanceof UserRecoverableAuthException) {
							startActivityForResult(
									((UserRecoverableAuthException) e)
											.getIntent(), REQUEST_AUTHORIZATION);
						}
					}

					mDriveService = getDriveService(mCredential);
				}
			}).start();

		}

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		/** Painless networking with Volley */
		RequestManager.init(this);
		ImageCacheManager.getInstance().init(this, this.getPackageCodePath(),
				DISK_IMAGECACHE_SIZE, DISK_IMAGECACHE_COMPRESS_FORMAT,
				DISK_IMAGECACHE_QUALITY, CacheType.MEMORY);

		// Start Application services
		if (isMyServiceRunning(PlayerService.class.getName()))
			startService(new Intent(this, PlayerService.class));

		if (isMyServiceRunning(HTTPDService.class.getName())) {
			startService(new Intent(this, HTTPDService.class));

			Intent bindIntent = new Intent(this, HTTPDService.class);
			bindService(bindIntent, mHTTPDServiceConnection,
					Context.BIND_AUTO_CREATE);
		}

		currentTheme = ThemeHelper.getTheme(prefs);
		setTheme(currentTheme);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mRemoteControlResponder = new ComponentName(getPackageName(),
				HeadsetReceiver.class.getName());

		IntentFilter receiverFilter = new IntentFilter(
				Intent.ACTION_HEADSET_PLUG);
		receiver = new HeadsetReceiver();
		registerReceiver(receiver, receiverFilter);

		ControlButtons.setThemeHelper(getApplicationContext());

		mFragmentManager = getSupportFragmentManager(); // getSupportFragmentManager();

		if (debugging)
			mFragmentManager.enableDebugLogging(true);

		mFragmentTransition = mFragmentManager.beginTransaction();
		mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);

		/*
		 * TabPageIndicator indicator = (TabPageIndicator)
		 * findViewById(R.id.info); indicator.setViewPager(pager);
		 */

		// PodcastUpdateManager.setUpdate(this);

		// set the Behind View
		if (SHOW_DRAWER) {
			mTitle = mDrawerTitle = getTitle();
			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
					R.drawable.ic_drawer, R.string.drawer_open,
					R.string.drawer_close) {

				/**
				 * Called when a drawer has settled in a completely closed
				 * state.
				 */
				public void onDrawerClosed(View view) {
					getActionBar().setTitle(mTitle);
					invalidateOptionsMenu(); // creates call to
												// onPrepareOptionsMenu()
				}

				/** Called when a drawer has settled in a completely open state. */
				public void onDrawerOpened(View drawerView) {
					getActionBar().setTitle(mDrawerTitle);
					invalidateOptionsMenu(); // creates call to
												// onPrepareOptionsMenu()
				}
			};

			// Set the drawer toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);

			mPlanetTitles = getResources().getStringArray(
					R.array.entries_item_expire);
			mDrawerList = (LinearLayout) findViewById(R.id.left_drawer);

			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
			mDrawerLayout, /* DrawerLayout object */
			R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
			R.string.drawer_open, /* "open drawer" description */
			R.string.drawer_close /* "close drawer" description */
			) {

				/**
				 * Called when a drawer has settled in a completely closed
				 * state.
				 */
				public void onDrawerClosed(View view) {
					getActionBar().setTitle(mTitle);
				}

				/** Called when a drawer has settled in a completely open state. */
				public void onDrawerOpened(View drawerView) {
					getActionBar().setTitle(mDrawerTitle);
				}
			};

			// Set the drawer toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);

			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeButtonEnabled(true);

		}

		// Set up the ViewPager with the sections adapter.
		mViewPager.setAdapter(mSectionsPagerAdapter);
		/*
		 * }
		 * 
		 * public void onActivityCreated(Bundle savedInstanceState) {
		 */
		if (debugging) {
			PodcastDownloadManager.cancelAllDownloads(this
					.getApplicationContext());
		}

		if (prefs.getBoolean(SettingsActivity.CLOUD_SUPPORT, true)) {
			startGoogleReader();
		}
	}

	/**
	 * Set the current theme based on the preference
	 */
	private void refreshTheme() {
		if (currentTheme != ThemeHelper.getTheme(prefs)) {
			recreate();
		}
	}

	/**
	 * Test if a service is running
	 * 
	 * @param serviceName
	 *            MyService.class.getName()
	 * @return
	 */
	private boolean isMyServiceRunning(String serviceName) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceName.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public static GoogleAccountCredential getCredentials() {
		return mCredential;
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == RESULT_OK && data != null
					&& data.getExtras() != null) {
				String accountName = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					prefs.edit().putString(ACCOUNT_KEY, accountName).commit();
					mCredential.setSelectedAccountName(accountName);
					mDriveService = getDriveService(mCredential);
					// startCameraIntent();
					// updateDatabase();
					Account account = mCredential.getSelectedAccount();
					Bundle bundle = new Bundle();
					bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED,
							true);
					bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
					ContentResolver.requestSync(account,
							"org.bottiger.podcast.provider.podcastprovider",
							bundle);
				}
			}
			break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				// saveFileToDrive();
			} else {
				startActivityForResult(mCredential.newChooseAccountIntent(),
						REQUEST_ACCOUNT_PICKER);
			}
			break;
		}
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
		unregisterReceiver(receiver);
		BugSenseHandler.closeSession(MainActivity.this);
	}

	@Override
	public void onItemSelected(long id) {
		SubscriptionFeedID = id;
		mSectionsPagerAdapter.notifyDataSetChanged();
		mViewPager.setCurrentItem(2);
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshTheme();
		if (mInit) {
			mInit = false;

			if (mCursor != null)
				mCursor.close();
		}

		PodcastService.setAlarm(this);

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mInit = true;

		log.debug("onLowMemory()");
		// finish();
	}

	/**
	 * Creates the actionbar from the XML menu file. In addition it makes sure
	 * the play/pause icon is correct
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_swipe, menu);
		ThemeHelper themeHelper = new ThemeHelper(this);

		// The player can only be playing if the PlayerService has been bound
		MenuItem menuItem = menu.findItem(R.id.menu_control);
		if (PodcastBaseFragment.mPlayerServiceBinder != null) {

			if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
				menuItem.setIcon(themeHelper.getAttr(R.attr.pause_invert_icon));
			} else if (PodcastBaseFragment.mPlayerServiceBinder.isOnPause()) {
				menuItem.setIcon(themeHelper.getAttr(R.attr.play_invert_icon));
			} else {
				menuItem.setVisible(false);
			}
		} else {
			menuItem.setVisible(false);
		}

		MenuItem menuItemSync = menu.findItem(R.id.menu_sync);
		menuItemSync.setIcon(themeHelper.getAttr(R.attr.sync_icon));
		return true;
	}

	/**
	 * Right corner menu options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_control:
			if (PodcastBaseFragment.mPlayerServiceBinder != null) {
				PodcastBaseFragment.mPlayerServiceBinder.toggle();
			}
			return true;
		case R.id.menu_add:
			// mDriveUtils.driveAccount();
			AddPodcastDialog.addPodcast(this);
			return true;
		case R.id.menu_sync:

			if (prefs.getBoolean(SettingsActivity.CLOUD_SUPPORT, true)) {
			// Google Drive Sync mDriveUtils = new DriveUtils(this);
			Account account = mCredential.getSelectedAccount();
			Bundle bundle = new Bundle();
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			String auth = PodcastProvider.AUTHORITY;
			auth = "org.bottiger.podcast.provider.PodcastProvider"; //
			ContentResolver.requestSync(account,
					"org.bottiger.podcast.provider.podcastprovider", bundle);
			ContentResolver.requestSync(account, auth, bundle);
			} else {
				CharSequence text = "Please enabled cloud support in the settings menu before attempting to sync";
				int duration = Toast.LENGTH_LONG;

				Toast toast = Toast.makeText(this, text, duration);
				toast.show();
			}

			return true;
		case R.id.menu_settings:
			Intent i = new Intent(getBaseContext(), SettingsActivity.class);
			startActivity(i);
			return true;
		case R.id.menu_refresh:
			PodcastDownloadManager.start_update(this);
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
			log.debug("inside: getItem(" + i + ")");
			if (i == 1) {
				fragment = getSubscriptionFragmentContent();
			} else if (i == 0) {
				fragment = new RecentItemFragment(); // new
														// DummySectionFragment();
			} else if (i == 2) {
				fragment = getFeedFragmentContent();
			} else {
				fragment = new DSLVFragmentBGHandle();
				// fragment = new DummySectionFragment();
				// fragment = new PlayerFragment();
			}
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return (SubscriptionFeedID == 0) ? 2 : 3;
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
		log.debug("inside: getSubscriptionFragmentContent()");
		return new SubscriptionsFragment();
	}

	private Fragment getFeedFragmentContent() {
		Subscription sub = Subscription.getById(getContentResolver(),
				SubscriptionFeedID);
		return FeedFragment.newInstance(sub);
	}

	private Drive getDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
				new GsonFactory(), credential).build();
	}

	private void startGoogleReader() {
		try {
			Account[] a = AccountManager.get(getApplicationContext())
					.getAccountsByType("com.google");
			MainActivity.mAccount = a[0];
			gReader = new GoogleReader(this, mAccount,
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

}
