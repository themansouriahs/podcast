package org.bottiger.podcast;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.bottiger.podcast.activities.intro.Intro;
import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.TransitionUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.dialogs.DialogAddPodcast;

import java.io.IOException;

// Sliding
public class MainActivity extends FragmentContainerActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static boolean showIntro = true;

    private PreferenceHelper mPreferenceHelper = new PreferenceHelper();

    private HeadsetReceiver receiver;
	private SharedPreferences prefs;

	private int currentTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "App start time: " + System.currentTimeMillis());
		super.onCreate(savedInstanceState);

		boolean firstRun = SoundWaves.getAppContext(this).IsFirstRun();

		if (firstRun && !showIntro) {
			VendorCrashReporter.report("ShowWrongIntro", "Something is wrong");
		}

		if (firstRun && showIntro) {
			showIntro = false;
			Intent intent = new Intent(MainActivity.this, Intro.class);
			startActivity(intent);
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		incrementAppStarts(this, prefs);

		currentTheme = UIUtils.getTheme(this);

		if (ApplicationConfiguration.TRACE_STARTUP) {
			//ViewServer.get(this).addWindow(this);
			// Tracing is buggy on emulator
			Debug.startMethodTracing("calc");

		}

		if (BuildConfig.DEBUG && false) {
			try {
				SqliteCopy.backupDatabase();
			} catch (IOException e) { // TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


        mPreferenceHelper.setOrientation(this, prefs);


		IntentFilter receiverFilter = new IntentFilter(
				AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		receiver = new HeadsetReceiver();
		registerReceiver(receiver, receiverFilter);

		//if (Build.VERSION.SDK_INT >= 23) {
		//	CloudSyncUtils.startCloudSync(this);
		//}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
        super.onDestroy();
	}

	@Override
	public void onResume() {
		if (currentTheme != UIUtils.getTheme(this)) {
			recreate();
		}

        super.onResume();
	}

    @Override
    protected void onPause() {
        if (ApplicationConfiguration.TRACE_STARTUP)
            Debug.stopMethodTracing();
        super.onPause();
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

			case R.id.menu_web_player: {
				TransitionUtils.startWebScannerActivity(this);
				return true;
			}

		}

		return super.onOptionsItemSelected(item);
	}

	private static void incrementAppStarts(@NonNull Context argContext, @NonNull SharedPreferences argSharedPreferences) {
		String startCountKey = argContext.getString(R.string.pref_app_start_count_key);
		long startCount = argSharedPreferences.getLong(startCountKey, 0) + 1;
		argSharedPreferences.edit().putLong(startCountKey, startCount).apply();
	}
}
