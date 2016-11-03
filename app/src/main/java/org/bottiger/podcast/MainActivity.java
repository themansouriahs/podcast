package org.bottiger.podcast;

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
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.syncadapter.CloudSyncUtils;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.TransitionUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.dialogs.DialogAddPodcast;
import org.bottiger.podcast.webservices.datastore.webplayer.WebPlayerAuthenticator;

import java.io.IOException;
import java.text.ParseException;

// Sliding
public class MainActivity extends FragmentContainerActivity {

	private static final String TAG = "MainActivity";

    private PreferenceHelper mPreferenceHelper = new PreferenceHelper();

    private HeadsetReceiver receiver;
	private SharedPreferences prefs;

	private int currentTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "App start time: " + System.currentTimeMillis());
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
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

		// Start Application services
		if (!isMyServiceRunning(PlayerService.class.getName()))
			startService(new Intent(this, PlayerService.class));

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
	    super.onPause();
        if (ApplicationConfiguration.TRACE_STARTUP)
            Debug.stopMethodTracing();
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

			/*
			case R.id.menu_web_player: {
				TransitionUtils.openWebPlayerAuthenticator(this);
				return true;
			}
			*/
		}

		return super.onOptionsItemSelected(item);
	}

	// Get the results:
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if(result != null) {
			if(result.getContents() == null) {
				Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
				IEpisode episode = SoundWaves.getAppContext(this).getPlaylist().getItem(0);
				try {
					WebPlayerAuthenticator.authenticate(result.getContents(), this, episode);
				} catch (ParseException e) {
					e.printStackTrace();
					Toast.makeText(this, "ParseError: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
