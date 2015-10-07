package org.bottiger.podcast;

import java.io.IOException;

import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.syncadapter.CloudSyncUtils;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.views.dialogs.DialogAddPodcast;
import org.bottiger.podcast.utils.ThemeHelper;
import org.bottiger.podcast.utils.TransitionUtils;


import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.squareup.otto.Subscribe;

// Sliding
public class MainActivity extends FragmentContainerActivity {

	private static final String TAG = "MainActivity";

    static PreferenceHelper mPreferenceHelper = new PreferenceHelper();

    private HeadsetReceiver receiver;

	private SharedPreferences prefs;

	private int currentTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Start the player service
		((SoundWaves)getApplicationContext()).startService();

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

		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mPreferenceHelper.setOrientation(this, prefs);

		// Start Application services
		if (!isMyServiceRunning(PlayerService.class.getName()))
			startService(new Intent(this, PlayerService.class));

		currentTheme = ThemeHelper.getTheme(prefs);
		setTheme(currentTheme);

		/*
		IntentFilter receiverFilter = new IntentFilter(
				Intent.ACTION_HEADSET_PLUG); */
		IntentFilter receiverFilter = new IntentFilter(
				AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		receiver = new HeadsetReceiver();
		registerReceiver(receiver, receiverFilter);

		if (Build.VERSION.SDK_INT >= 23) {
			CloudSyncUtils.startCloudSync(this);
		}

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
	public void onResume() {
        super.onResume();
		refreshTheme();
	}

    @Override
    protected void onPause() {
        super.onPause();
        if (ApplicationConfiguration.TRACE_STARTUP)
            Debug.stopMethodTracing();
    }

	@Override
	public void onLowMemory() {
        super.onLowMemory();
	}

	@Override
	public void onTrimMemory(int level) {
        super.onTrimMemory(level);
		if (level >= TRIM_MEMORY_MODERATE ) {
			// Clear fresco cache
			ImagePipeline imagePipeline = Fresco.getImagePipeline();
			imagePipeline.clearMemoryCaches();
		}
	}

	/**
	 * Creates the actionbar from the XML menu file. In addition it makes sure
	 * the play/pause icon is correct
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_swipe, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Right corner menu options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_add: {
				DialogAddPodcast.addPodcast(this);
				return true;
			}
			case R.id.menu_settings: {
				TransitionUtils.openSettings(this);
				return true;
			}
			case R.id.menu_refresh: {
				SoundWaves.sSubscriptionRefreshManager.refreshAll();
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}
}
