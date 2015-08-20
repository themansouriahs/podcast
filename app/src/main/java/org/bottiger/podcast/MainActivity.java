package org.bottiger.podcast;

import java.io.IOException;

import org.bottiger.podcast.AbstractEpisodeFragment.OnPlaylistRefreshListener;
import org.bottiger.podcast.cloud.CloudProvider;
import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.HTTPDService;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.PodcastService;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.views.dialogs.DialogAddPodcast;
import org.bottiger.podcast.utils.ThemeHelper;
import org.bottiger.podcast.utils.TransitionUtils;


import android.accounts.Account;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
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
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.google.api.client.json.gson.GsonFactory;

// Sliding
public class MainActivity extends FragmentContainerActivity implements
		OnPlaylistRefreshListener {

	private static final String ACCOUNT_KEY = "account";

	private final int REQUEST_AUTHORIZATION = 1;
	private final int REQUEST_ACCOUNT_PICKER = 0;

	public static PodcastService mPodcastServiceBinder = null;
	public static HTTPDService mHTTPDServiceBinder = null;

	static boolean mBound = false;

    static PreferenceHelper mPreferenceHelper = new PreferenceHelper();

	// public static GoogleReader gReader = null;
	public static CloudProvider gReader = null;

	protected static Cursor mCursor = null;
	protected boolean mInit = false;
	public static Account mAccount;

    private HeadsetReceiver receiver;

	private SharedPreferences prefs;

	private int currentTheme;

	public static ServiceConnection mHTTPDServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mHTTPDServiceBinder = ((HTTPDService.HTTPDBinder) service)
					.getService();
			String out = mHTTPDServiceBinder.getTest();
			// android.util.Log.d("nanoHTTPD", out);

			// init webserver

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mHTTPDServiceBinder = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Start the player service
		((SoundWaves)getApplicationContext()).startService();

		/*
		 * BugSenseHandler.initAndStartSession(MainActivity.this, ((SoundWaves)
		 * this.getApplication()).getBugSenseAPIKey());
		 */
		// 3132
		//PodcastOpenHelper helper = new PodcastOpenHelper(this);
		//helper.getWritableDatabase().execSQL("update subscriptions set status = " + Subscription.STATUS_SUBSCRIBED);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (ApplicationConfiguration.TRACE_STARTUP) {
			//ViewServer.get(this).addWindow(this);
			// Tracing is buggy on emulator
			Debug.startMethodTracing("calc");

		}
		
		if (BuildConfig.DEBUG) {
			try {
				SqliteCopy.backupDatabase();
			} catch (IOException e) { // TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (prefs.getBoolean(SettingsActivity.CLOUD_SUPPORT, false) && false) {

            /*
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
						""));*/

            /*
            GOogle flavor

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
			}); //.start();
			*/

		}

		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mPreferenceHelper.setOrientation(this, prefs);

		/** Painless networking with Volley */
		//RequestManager.init(this);
		//ImageCacheManager.getInstance().init(this, this.getPackageCodePath(),
		//		DISK_IMAGECACHE_SIZE, DISK_IMAGECACHE_COMPRESS_FORMAT,
		//		DISK_IMAGECACHE_QUALITY, CacheType.MEMORY);

		// Start Application services
		if (!isMyServiceRunning(PlayerService.class.getName()))
			startService(new Intent(this, PlayerService.class));

        /*
		if (!isMyServiceRunning(HTTPDService.class.getName())) {
			startService(new Intent(this, HTTPDService.class));

			Intent bindIntent = new Intent(this, HTTPDService.class);
			bindService(bindIntent, mHTTPDServiceConnection,
					Context.BIND_AUTO_CREATE);
		}*/

		currentTheme = ThemeHelper.getTheme(prefs);
		setTheme(currentTheme);

		/*
		IntentFilter receiverFilter = new IntentFilter(
				Intent.ACTION_HEADSET_PLUG); */
		IntentFilter receiverFilter = new IntentFilter(
				AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		receiver = new HeadsetReceiver();
		registerReceiver(receiver, receiverFilter);

	}

    /**
     * Return a reference to the playerservice if bound
     */
    @Nullable
    public static PlayerService getPlayerService() {
        return SoundWaves.sBoundPlayerService;
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

    /*
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
					//mDriveService = getDriveService(mCredential);
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
	}*/

	@Override
	protected void onPause() {
		super.onPause();
		if (ApplicationConfiguration.TRACE_STARTUP)
			Debug.stopMethodTracing();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);

        try {
            unbindService(((SoundWaves)getApplicationContext()).playerServiceConnection);
        } catch (Exception e) {
            VendorCrashReporter.handleException(e);
        }
		super.onDestroy();
	}

	@Override
	public void onRefreshPlaylist() {
		//mSectionsPagerAdapter.refreshData(SectionsPagerAdapter.PLAYLIST);
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

		PodcastService.setupAlarm(this);

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

		// The extended_player can only be playing if the PlayerService has been bound
        /*
		MenuItem menuItem = menu.findItem(R.id.menu_control);
		if (PodcastBaseFragment.sBoundPlayerService != null) {

			if (PodcastBaseFragment.sBoundPlayerService.isPlaying()) {
				menuItem.setIcon(themeHelper.getAttr(R.attr.pause_invert_icon));
			} else if (PodcastBaseFragment.sBoundPlayerService.isOnPause()) {
				menuItem.setIcon(themeHelper.getAttr(R.attr.play_invert_icon));
			} else {
				//menuItem.setVisible(false);
			}
		} else {
			//menuItem.setVisible(false);
		}*/

		return super.onCreateOptionsMenu(menu);

	}

	/**
	 * Right corner menu options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            /*
		case R.id.menu_control:
			if (PodcastBaseFragment.sBoundPlayerService != null) {
				PodcastBaseFragment.sBoundPlayerService.toggle();
			}
			return true;*/
		case R.id.menu_add:
			// mDriveUtils.driveAccount();
			DialogAddPodcast.addPodcast(this);
			return true;
		case R.id.menu_settings:
            TransitionUtils.openSettings(this);
			return true;
		case R.id.menu_refresh:
			SoundWaves.sSubscriptionRefreshManager.refreshAll();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    /*
	private Drive getDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
				new GsonFactory(), credential).build();
	}*/

}
