package org.bottiger.podcast.service;

import org.bottiger.podcast.SettingsActivity;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.PodcastUpdateReceiver;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.utils.PodcastLog;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class PodcastService extends IntentService {

	public PodcastService() {
		super("PodcastService");
	}

	// from
	// http://it-ride.blogspot.dk/2010/10/android-implementing-notification.html
	private WakeLock mWakeLock;
	private final String TAG = "wakelock";

	public static final int UPDATE_PODCAST = 100;

	private final PodcastLog log = PodcastLog.getLog(getClass());

	public static final String UPDATE_DOWNLOAD_STATUS = PodcastService.class
			.getName() + ".UPDATE_DOWNLOAD_STATUS";

	public static final int NO_CONNECT = 1;
	public static final int WIFI_CONNECT = 2;
	public static final int MOBILE_CONNECT = 4;

	public int pref_connection_sel = MOBILE_CONNECT | WIFI_CONNECT;

	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;
	private static final long ONE_DAY = 24L * ONE_HOUR;

	public long pref_update_wifi = 0;
	public long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 1000;
	public long pref_played_file_expire = 0;
	public int pref_max_valid_size = 1000;

	private PodcastUpdateReceiver updateManager = new PodcastUpdateReceiver();
	private EpisodeDownloadManager pdm = new EpisodeDownloadManager();

	// @Override
	public void onStart(Context context, Intent intent, int startId) {
		super.onStart(intent, startId);
		PodcastUpdateReceiver.updateNow(context); // new AlarmManager way
		log.debug("onStart()");
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		log.debug("onLowMemory()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private final IBinder binder = new PodcastBinder();

	public class PodcastBinder extends Binder {
		public PodcastService getService() {
			return PodcastService.this;
		}
	}

	/**
	 * Creates and intent to be executed when the alarm goes of.
	 * 
	 * @param context
	 * @return The intent to be executed when the alarm fires
	 */
	public static PendingIntent getAlarmIntent(Context context) {
		Intent i = new Intent(context, PodcastService.class);

		/*
		 * Not used at the moment Bundle bundle = new Bundle();
		 * bundle.putLong("intervalMinutes", alarmInterval(intervalMinutes));
		 * i.putExtra("schedule", bundle);
		 */

		return PendingIntent.getService(context, 0, i, 0);
	}

	/**
	 * Setup a pending intent for the next alarm and schedules it
	 * 
	 * @param context
	 */
	public static void setupAlarm(Context context) {
		// Refresh interval
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		long minutes = prefs.getLong("interval", 60);

		PendingIntent pi = getAlarmIntent(context);
		setAlarm(context, pi, 15, minutes);
	}

	/**
	 * Schedules a refresh every X minutes. Where X is defined by the user in
	 * the settings.
	 * 
	 * @param context
	 */
	public static void setAlarm(Context context, PendingIntent pi,
			long nextAlarmMinutes, long intervalMinutes) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pi);

		// by my own convention, minutes <= 0 means notifications are disabled
		if (nextAlarmMinutes > 0) {
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					nextAlarm(nextAlarmMinutes),
					alarmInterval(intervalMinutes), pi);
		}
	}

	public static long nextAlarm(long minutes) {
		return SystemClock.elapsedRealtime() + alarmInterval(minutes);
	}

	public static long alarmInterval(long minutes) {
		return Long.valueOf(minutes * 60 * 1000); // minutes to milliseconds
	}

	public void updateSetting() {
		SharedPreferences pref = getSharedPreferences(
				SettingsActivity.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);

		boolean b = pref.getBoolean("pref_download_only_wifi", false);
		pref_connection_sel = b ? WIFI_CONNECT
				: (WIFI_CONNECT | MOBILE_CONNECT);

		pref_update_wifi = Integer.parseInt(pref.getString("pref_update_wifi",
				"60"));
		pref_update_wifi *= ONE_MINUTE;

		pref_update_mobile = Integer.parseInt(pref.getString(
				"pref_update_mobile", "120"));
		pref_update_mobile *= ONE_MINUTE;

		pref_item_expire = Integer.parseInt(pref.getString("pref_item_expire",
				"7"));
		pref_item_expire *= ONE_DAY;
		pref_download_file_expire = Integer.parseInt(pref.getString(
				"pref_download_file_expire", "7"));
		pref_download_file_expire *= ONE_DAY;
		pref_played_file_expire = Integer.parseInt(pref.getString(
				"pref_played_file_expire", "24"));
		pref_played_file_expire *= ONE_HOUR;

		pref_max_valid_size = Integer.parseInt(pref.getString(
				"pref_max_new_items", "10"));
	}

	/**
	 * This is where we initialize. We call this when onStart/onStartCommand is
	 * called by the system. We won't do anything with the intent here, and you
	 * probably won't, either.
	 */
	private void handleIntent(Intent intent) {
		// obtain the wake lock
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWakeLock.acquire();

		// check the global background data setting
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (!cm.getBackgroundDataSetting()) {
			stopSelf();
			return;
		}

		// do the actual work, in a separate thread
		// Bundle bb = intent.getExtras().getBundle("b");
		// String s = bb.getString("store");
		new PollTask().execute();
	}

	private class PollTask extends AsyncTask<Void, Void, Void> {
		/**
		 * This is where YOU do YOUR work. There's nothing for me to write here
		 * you have to fill this in. Make your HTTP request(s) or whatever it is
		 * you have to do to get your updates in here, because this is run in a
		 * separate thread
		 */
		@Override
		protected Void doInBackground(Void... params) {
            SubscriptionRefreshManager subscriptionRefreshManager = new SubscriptionRefreshManager(PodcastService.this);
            subscriptionRefreshManager.refreshAll();
			return null;
		}

		/**
		 * In here you should interpret whatever you fetched in doInBackground
		 * and push any notifications you need to the status bar, using the
		 * NotificationManager. I will not cover this here, go check the docs on
		 * NotificationManager.
		 * 
		 * What you HAVE to do is call stopSelf() after you've pushed your
		 * notification(s). This will: 1) Kill the service so it doesn't waste
		 * precious resources 2) Call onDestroy() which will release the wake
		 * lock, so the device can go to sleep again and save precious battery.
		 */
		@Override
		protected void onPostExecute(Void result) {
			// handle your data
			stopSelf();
		}
	}

	/**
	 * This is called on 2.0+ (API level 5 or higher). Returning
	 * START_NOT_STICKY tells the system to not restart the service if it is
	 * killed because of poor resource (memory/cpu) conditions.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent(intent);

		return START_NOT_STICKY;
	}

	/**
	 * In onDestroy() we release our wake lock. This ensures that whenever the
	 * Service stops (killed for resources, stopSelf() called, etc.), the wake
	 * lock will be released.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mWakeLock.release();
		log.debug("onDestroy()");
	}

	/**
	 * Not sure if this is ever called
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
	}

}
