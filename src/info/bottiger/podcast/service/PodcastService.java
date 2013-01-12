package info.bottiger.podcast.service;

import java.util.PriorityQueue;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import info.bottiger.podcast.Pref;
import info.bottiger.podcast.R;
import info.bottiger.podcast.fetcher.FeedFetcher;
import info.bottiger.podcast.parser.FeedHandler;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.provider.SubscriptionColumns;
import info.bottiger.podcast.utils.LockHandler;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.SDCardManager;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

public class PodcastService extends Service {
	
	private final Log log = Log.getLog(getClass());

	public static final String UPDATE_DOWNLOAD_STATUS = PodcastService.class
			.getName() + ".UPDATE_DOWNLOAD_STATUS";
	
	public static final int NO_CONNECT = 1;
	public static final int WIFI_CONNECT = 2;
	public static final int MOBILE_CONNECT = 4;

	private static final int MSG_TIMER = 0;

	public int pref_connection_sel = MOBILE_CONNECT | WIFI_CONNECT;
	
	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;
	private static final long ONE_DAY = 24L * ONE_HOUR;

	// private static final long timer_freq = 3 * ONE_MINUTE;
	private static final long timer_freq = ONE_HOUR;
	private long pref_update = 2 * 60 * ONE_MINUTE;

	
	public long pref_update_wifi = 0;
	public long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 1000;
	public long pref_played_file_expire = 0;
	public int pref_max_valid_size = 1000;


	
	private PodcastUpdateManager updateManager = new PodcastUpdateManager();
	private PodcastDownloadManager pdm = new PodcastDownloadManager();

	/*
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMER:
				log.debug("Message: MSG_TIMER.");

				//start_update();
				//removeExpires();
				//do_download(false);

				//long nextUpdate = (PodcastService.mDownloadQueue.isEmpty()) ? timer_freq
				//		: 1;
				//triggerNextTimer(nextUpdate);

				break;
			}
		}
	};

	private void triggerNextTimer(long delay) {
		Message msg = Message.obtain();
		msg.what = MSG_TIMER;
		handler.sendMessageDelayed(msg, delay);
	}
	*/

	

	@Override
	public void onCreate() {
		// Podcast service onCreate()
		super.onCreate();
		//updateSetting(); //removed - not sure if I should
		SDCardManager.getSDCardStatusAndCreate();
		
		// old Alarm way
		//triggerNextTimer(1);

	}

	//@Override
	public void onStart(Context context, Intent intent, int startId) {
		super.onStart(intent, startId);
		this.updateManager.updateNow(context); // new AlarmManager way
		log.debug("onStart()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		log.debug("onDestroy()");
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

	public void start_download() {
		pdm.do_download(true, getBaseContext());
	}
	
	public void start_update() {
		pdm.start_update(getBaseContext());
	}
	
	public void start_update(PullToRefreshListView pullToRefreshView) {
		pdm.start_update(getBaseContext(), pullToRefreshView);
	}

	public void updateSetting() {
		SharedPreferences pref = getSharedPreferences(
				Pref.HAPI_PREFS_FILE_NAME, Service.MODE_PRIVATE);

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
	
	public void downloadItem(ContentResolver context, FeedItem item) {
		item.prepareDownload(context);
		pdm.addItemToQueue(item);
		pdm.do_download(true, getBaseContext());
	}


	public FeedItem getDownloadingItem() {
		return pdm.getDownloadingItem();
	}

}
