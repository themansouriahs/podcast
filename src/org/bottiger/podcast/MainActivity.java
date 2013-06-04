package org.bottiger.podcast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import org.bottiger.podcast.cloud.CloudProvider;
import org.bottiger.podcast.cloud.GoogleReader;
import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.provider.PodcastProvider;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.HTTPDService;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.service.PodcastService;
import org.bottiger.podcast.utils.ControlButtons;
import org.bottiger.podcast.utils.DriveUtils;
import org.bottiger.podcast.utils.Log;
import org.bottiger.podcast.utils.SDCardManager;
import org.bottiger.podcast.utils.SlidingMenuBuilder;
import org.bottiger.podcast.utils.ThemeHelper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

// Sliding
public class MainActivity extends FragmentActivity implements
		OnItemSelectedListener {

	private static final String ACCOUNT_KEY = "account";

	private Drive mDriveService = null;
	private final int REQUEST_AUTHORIZATION = 1;
	private final int REQUEST_ACCOUNT_PICKER = 0;
	private static GoogleAccountCredential mCredential;

	public static PodcastService mPodcastServiceBinder = null;
	public static HTTPDService mHTTPDServiceBinder = null;

	static boolean mBound = false;
	
	/** Painless networking with Volley */
	private RequestQueue mRequestQueue;

	// public static GoogleReader gReader = null;
	public static CloudProvider gReader = null;

	protected static Cursor mCursor = null;
	protected boolean mInit = false;
	protected final Log log = Log.getDebugLog(getClass(), 0);
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

	private DriveUtils mDriveUtils;

	public static ServiceConnection mHTTPDServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mHTTPDServiceBinder = ((HTTPDService.HTTPDBinder) service)
					.getService();
			// String out = mHTTPDServiceBinder.getTest();
			// android.util.Log.d("nanoHTTPD", out);

			new Thread(new Runnable() {
				public void run() {
					URL url = null;
					HttpURLConnection connection = null;
					try {
						url = new URL("http://127.0.0.1:8080/test");
						connection = (HttpURLConnection) url.openConnection();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					InputStream in = null;
					try {
						// Read the response.
						in = connection.getInputStream();
						// byte[] response = readFully(in);
						// return new String(response, "UTF-8");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					InputStream bin = new BufferedInputStream(in);

					BufferedReader r = new BufferedReader(
							new InputStreamReader(bin));

					StringBuilder total = new StringBuilder();
					String line;
					try {
						while ((line = r.readLine()) != null) {
							total.append(line);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					String res = total.toString();
					res = res + "sdfd";
					res = res + "123";

				}
			}).start();

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mHTTPDServiceBinder = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(MainActivity.this,
				((SoundWaves) this.getApplication()).getBugSenseAPIKey());

		if (debugging) {
			// Tracing is buggy on emulator
			Debug.startMethodTracing("calc");

			if (true) {
				try {
					SqliteCopy.backupDatabase();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		mCredential = GoogleAccountCredential.usingOAuth2(this,
				"https://www.googleapis.com/auth/drive.file"); //"https://www.googleapis.com/auth/drive.appdata"); //DriveScopes.DRIVE);
		//mCredential = GoogleAccountCredential.usingOAuth2(this,
		//		DriveScopes.DRIVE_FILE); //DriveScopes.DRIVE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.contains(ACCOUNT_KEY))
			startActivityForResult(mCredential.newChooseAccountIntent(),
					REQUEST_ACCOUNT_PICKER);
		else
			mCredential
					.setSelectedAccountName(prefs.getString(ACCOUNT_KEY, ""));
		
		new Thread(new Runnable() {
			public void run() {
				try {
					mCredential.getToken();
				} catch (Exception e) {
					e.printStackTrace();
					if (e instanceof UserRecoverableAuthException) {
						startActivityForResult(
								((UserRecoverableAuthException) e).getIntent(),
								REQUEST_AUTHORIZATION);
					}
				}

				mDriveService = getDriveService(mCredential);
			}
		}).start();

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		mRequestQueue = Volley.newRequestQueue(this);

		// Start Application services
		startService(new Intent(this, PlayerService.class));
		startService(new Intent(this, HTTPDService.class));

		Intent bindIntent = new Intent(this, HTTPDService.class);
		bindService(bindIntent, mHTTPDServiceConnection,
				Context.BIND_AUTO_CREATE);

		setContentView(R.layout.activity_swipe);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mRemoteControlResponder = new ComponentName(getPackageName(),
				HeadsetReceiver.class.getName());

		IntentFilter receiverFilter = new IntentFilter(
				Intent.ACTION_HEADSET_PLUG);
		HeadsetReceiver receiver = new HeadsetReceiver();
		registerReceiver(receiver, receiverFilter);

		ControlButtons.setThemeHelper(getApplicationContext());

		mFragmentManager = getSupportFragmentManager(); // getSupportFragmentManager();

		if (debugging)
			mFragmentManager.enableDebugLogging(true);

		mFragmentTransition = mFragmentManager.beginTransaction();
		mSectionsPagerAdapter = new SectionsPagerAdapter(mFragmentManager);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(mSectionsPagerAdapter);
		pager.setOffscreenPageLimit(3);

		/*
		 * TabPageIndicator indicator = (TabPageIndicator)
		 * findViewById(R.id.info); indicator.setViewPager(pager);
		 */

		// PodcastUpdateManager.setUpdate(this);

		// FIXME

		// set the Behind View
		// setBehindContentView(R.layout.download);

		// SlidingMenu menu = getSlidingMenu();
		SlidingMenuBuilder.build(this, mSectionsPagerAdapter);
		// setSlidingActionBarEnabled(true);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
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

	/*
	 * @Override protected void onActivityResult(final int requestCode, final
	 * int resultCode, final Intent data) {
	 * mDriveUtils.activityResult(requestCode, resultCode, data); }
	 */

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
					bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
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
		if (mInit) {
			mInit = false;

			if (mCursor != null)
				mCursor.close();

			// unbindService(serviceConnection);

			// startInit(); FIXME

		}

		PodcastService.setAlarm(this);
		/*
		 * prefs = PreferenceManager.getDefaultSharedPreferences(this); int
		 * minutes = prefs.getInt("interval", 1); AlarmManager am =
		 * (AlarmManager) getSystemService(ALARM_SERVICE); Intent intent = new
		 * Intent(this, PodcastService.class); PendingIntent pi =
		 * PendingIntent.getService(this, 0, intent, 0); am.cancel(pi); // by my
		 * own convention, minutes <= 0 means notifications are disabled if
		 * (minutes > 0) {
		 * am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
		 * SystemClock.elapsedRealtime() + minutes*60*1000, minutes*60*1000,
		 * pi); }
		 */

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

		// The player can only be playing if the PlayerService has been bound
		MenuItem menuItem = menu.findItem(R.id.menu_control);
		if (PodcastBaseFragment.mPlayerServiceBinder != null) {

			ThemeHelper themeHelper = new ThemeHelper(this);
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
			// Sync with google drive
			/*
			 * GoogleAccountCredential credential = GoogleAccountCredential
			 * .usingOAuth2(this, DriveScopes.DRIVE);
			 * 
			 * Account account = credential.getSelectedAccount(); if (account ==
			 * null) { final int REQUEST_ACCOUNT_PICKER = 1;
			 * startActivityForResult(credential.newChooseAccountIntent(),
			 * REQUEST_ACCOUNT_PICKER); account =
			 * credential.getSelectedAccount(); }
			 */
			// DriveSyncer mSyncer = new DriveSyncer(getApplicationContext());

			mDriveUtils = new DriveUtils(this);

			Account account = mCredential.getSelectedAccount();
			Bundle bundle = new Bundle();
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
			bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			String auth = PodcastProvider.AUTHORITY;
			auth = "org.bottiger.podcast.provider.PodcastProvider";
			// ContentResolver.requestSync(account,
			// "org.bottiger.podcast.provider.podcastprovider", bundle);
			ContentResolver.requestSync(account, auth, bundle);
			// mDriveUtils.driveAccount();
			// AddPodcastDialog.addPodcast(this);
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
				fragment = new RecentItemFragment();
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

	/**
	 * Retrieve a authorized service object to send requests to the Google Drive
	 * API. On failure to retrieve an access token, a notification is sent to
	 * the user requesting that authorization be granted for the
	 * {@code https://www.googleapis.com/auth/drive.file} scope.
	 * 
	 * @return An authorized service object.
	 */
	/*
	 * private Drive getDriveService(GoogleAccountCredential credential) { if
	 * (mDriveService == null) { try { /* GoogleAccountCredential credential =
	 * GoogleAccountCredential .usingOAuth2(getApplicationContext(),
	 * DriveScopes.DRIVE_FILE);
	 * 
	 * credential.setSelectedAccountName(mAccount.name); // Trying to get a
	 * token right away to see if we are authorized credential.getToken();
	 * mDriveService = new Drive.Builder( AndroidHttp.newCompatibleTransport(),
	 * new GsonFactory(), credential).build(); } catch (Exception e) { //
	 * Log.e(TAG, "Failed to get token"); // If the Exception is User
	 * Recoverable, we display a // notification that will trigger the // intent
	 * to fix the issue. if (e instanceof UserRecoverableAuthException) {
	 * 
	 * UserRecoverableAuthException exception = (UserRecoverableAuthException)
	 * e; NotificationManager notificationManager = (NotificationManager)
	 * getApplicationContext() .getSystemService(Context.NOTIFICATION_SERVICE);
	 * Intent authorizationIntent = exception.getIntent();
	 * 
	 * startActivityForResult(authorizationIntent, REQUEST_AUTHORIZATION);
	 * 
	 * authorizationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	 * .addFlags(Intent.FLAG_FROM_BACKGROUND); PendingIntent pendingIntent =
	 * PendingIntent.getActivity( getApplicationContext(),
	 * REQUEST_AUTHORIZATION, authorizationIntent, 0);
	 * 
	 * NotificationCompat.Builder notificationBuilder = new
	 * NotificationCompat.Builder( getApplicationContext())
	 * .setSmallIcon(android.R.drawable.ic_dialog_alert)
	 * .setTicker("Permission requested")
	 * .setContentTitle("Permission requested") .setContentText("for account " +
	 * mAccount.name) .setContentIntent(pendingIntent) .setAutoCancel(true);
	 * notificationManager.notify(0, notificationBuilder.build());
	 * 
	 * } else { e.printStackTrace(); } } } return mDriveService; }
	 */

}
