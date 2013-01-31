package info.bottiger.podcast.service;

import info.bottiger.podcast.R;
import info.bottiger.podcast.SwipeActivity;
import info.bottiger.podcast.parser.FeedParserWrapper;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.utils.LockHandler;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.SDCardManager;

import java.io.File;
import java.net.URL;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class PodcastDownloadManager {

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
	private static long pref_update = 2 * 60 * ONE_MINUTE;

	private static PriorityQueue<FeedItem> mDownloadQueue = new PriorityQueue<FeedItem>();
	private final Log log = Log.getLog(getClass());

	private static FeedItem mDownloadingItem = null;
	private static final LockHandler mDownloadLock = new LockHandler();

	private static final LockHandler mUpdateLock = new LockHandler();
	private static int mConnectStatus = NO_CONNECT;

	public static long pref_update_wifi = 0;
	public static long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 1000;
	public long pref_played_file_expire = 0;
	public int pref_max_valid_size = 20;

	public enum DownloadStatus {
		NOTHING, PENDING, DOWNLOADING, DONE, ERROR
	}

	private static DownloadManager downloadManager;
	private static long downloadReference;

	public static DownloadStatus getStatus(FeedItem item) {
		if (item == null)
			return DownloadStatus.NOTHING;

		if (mDownloadQueue.contains(item))
			return DownloadStatus.PENDING;

		if (mDownloadingItem != null)
			if (item.equals(mDownloadingItem))
				return DownloadStatus.DOWNLOADING;

		if (item.isDownloaded()) {
			return DownloadStatus.DONE;
		} else if (item.chunkFilesize > 0) {
			return DownloadStatus.ERROR;
			// consider deleting it here
		}

		return DownloadStatus.NOTHING;
	}

	public static void start_update(final Context context) {
		start_update(context, null);
	}

	public static void start_update(final Context context,
			final PullToRefreshListView pullToRefreshView) {
		if (updateConnectStatus(context) == NO_CONNECT)
			return;

		//log.debug("start_update()");
		if (mUpdateLock.locked() == false)
			return;

		new UpdateSubscriptions(context, pullToRefreshView).execute();
	}

	/**
	 * Download all the episodes in the queue
	 * 
	 * @param show
	 * @param context
	 */
	public static void startDownload(final Context context) {
		startDownload(false, context);
	}

	@SuppressLint("NewApi")
	@Deprecated
	public static void startDownload(boolean show, final Context context) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);

		if (SDCardManager.getSDCardStatusAndCreate() == false) {

			if (show)
				Toast.makeText(
						context,
						context.getResources()
								.getString(R.string.sdcard_unmout),
						Toast.LENGTH_LONG).show();
			return;
		}

		if (updateConnectStatus(context) == NO_CONNECT) {
			if (show)
				Toast.makeText(context,
						context.getResources().getString(R.string.no_connect),
						Toast.LENGTH_LONG).show();
			return;
		}

		/*
		 * Deprecated if (mDownloadLock.locked() == false) { int i = 5; i = i +
		 * 6; //return; }
		 */

		// new DownloadPodcast(context).execute();
		downloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);
		while (mDownloadQueue.size() > 0) {
			mDownloadingItem = getNextItem();
			Uri downloadURI = Uri.parse(mDownloadingItem.url);
			DownloadManager.Request request = new DownloadManager.Request(
					downloadURI);

			// Restrict the types of networks over which this download may
			// proceed.
			int networkType = DownloadManager.Request.NETWORK_WIFI;

			// Only Allow mobile network if the user has enabled it
			if (!sharedPreferences.getBoolean("pref_download_only_wifi", true))
				networkType = networkType
						| DownloadManager.Request.NETWORK_MOBILE;

			request.setAllowedNetworkTypes(networkType);
			// request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
			// | DownloadManager.Request.NETWORK_MOBILE);

			// request.setAllowedOverMetered(true);

			if (android.os.Build.VERSION.SDK_INT > 11)
				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

			// Set whether this download may proceed over a roaming connection.
			request.setAllowedOverRoaming(false);
			// Set the title of this download, to be displayed in notifications
			// (if
			// enabled).
			request.setTitle(mDownloadingItem.title);
			// Set a description of this download, to be displayed in
			// notifications
			// (if enabled)
			request.setDescription(mDownloadingItem.content);
			// Set the local destination for the downloaded file to a path
			// within
			// the application's external files directory
			// String downloadDir = SDCardManager.getDownloadDir();
			// String fileName = mDownloadingItem.getFilename();
			// request.setDestinationInExternalFilesDir(context,
			// downloadDir, fileName);
			File file = new File(mDownloadingItem.getAbsolutePath());
			request.setDestinationUri(Uri.fromFile(file));

			// Enqueue a new download and same the referenceId
			downloadReference = downloadManager.enqueue(request);
		}

	}

	/**
	 * Write this method
	 * 
	 * @param context
	 * @param cursor
	 */
	private void deleteExpireFile(Context context, Cursor cursor) {

		if (cursor == null)
			return;

		if (cursor.moveToFirst()) {
			do {
				FeedItem item = FeedItem.getByCursor(cursor);
				if (item != null) {
					item.delFile(context.getContentResolver());
				}
			} while (cursor.moveToNext());
		}
		cursor.close();

	}

	public void removeExpires(Context context) {
		long expiredTime = System.currentTimeMillis() - pref_item_expire;
		try {
			String where = ItemColumns.CREATED + "<" + expiredTime + " and "
					+ ItemColumns.STATUS + "<"
					+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW + " and "
					+ ItemColumns.LISTENED + "=0";

			context.getContentResolver().delete(ItemColumns.URI, where, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (SDCardManager.getSDCardStatus() == false) {
			return;
		}

		expiredTime = System.currentTimeMillis() - pref_download_file_expire;
		try {
			String where = ItemColumns.LAST_UPDATE + "<" + expiredTime
					+ " and " + ItemColumns.STATUS + ">"
					+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW + " and "
					+ ItemColumns.STATUS + "<="
					+ ItemColumns.ITEM_STATUS_PLAY_PAUSE + " and "
					+ ItemColumns.LISTENED + "=0";

			Cursor cursor = context.getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			deleteExpireFile(context, cursor);

		} catch (Exception e) {
			e.printStackTrace();
		}

		expiredTime = System.currentTimeMillis() - pref_played_file_expire;
		try {
			String where = ItemColumns.LAST_UPDATE + "<" + expiredTime
					+ " and " + ItemColumns.STATUS + ">"
					+ ItemColumns.ITEM_STATUS_PLAY_PAUSE + " and "
					+ ItemColumns.STATUS + "<"
					+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW + " and "
					+ ItemColumns.LISTENED + "=0";

			Cursor cursor = context.getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			deleteExpireFile(context, cursor);

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			String where = ItemColumns.STATUS + "="
					+ ItemColumns.ITEM_STATUS_DELETE;
			// DELETE status takes priority over KEEP flag

			Cursor cursor = context.getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			deleteExpireFile(context, cursor);

		} catch (Exception e) {
			e.printStackTrace();
		}

		String where = ItemColumns.STATUS + "="
				+ ItemColumns.ITEM_STATUS_DELETED;
		context.getContentResolver().delete(ItemColumns.URI, where, null);

	}

	private static int updateConnectStatus(Context context) {
		// log.debug("updateConnectStatus");
		try {

			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info == null) {
				mConnectStatus = NO_CONNECT;
				return mConnectStatus;
			}

			if (info.isConnected() && (info.getType() == 1)) {
				mConnectStatus = WIFI_CONNECT;
				pref_update = pref_update_wifi;
				return mConnectStatus;
			} else {
				mConnectStatus = MOBILE_CONNECT;
				pref_update = pref_update_mobile;

				return mConnectStatus;
			}
		} catch (Exception e) {
			e.printStackTrace();
			mConnectStatus = NO_CONNECT;

			return mConnectStatus;
		}

	}

	public static FeedItem getDownloadingItem() {
		return mDownloadingItem;
	}

	private static FeedItem getNextItem() {
		return mDownloadQueue.poll();
	}

	/**
	 * Add feeditem to the download queue
	 * 
	 * @param feedItem
	 */
	public static void addItemToQueue(FeedItem item) {
		mDownloadQueue.add(item);
	}

	/**
	 * Update the list of subscriptions as well as their content
	 * 
	 * @author Arvid Böttiger
	 */
	private static class UpdateSubscriptions extends
			AsyncTask<Void, Subscription, PullToRefreshListView> {
		Context mContext;
		PullToRefreshListView mRefreshView;
		AsyncTask<URL, Void, Void> subscriptionDownloader;

		/*
		 * AsyncTask may be asynchronous, but not very concurrent. Instead of
		 * spawning a bunch of AsyncTasks to refresh our feeds we use a
		 * ThreadPool
		 * 
		 * http://stackoverflow.com/questions/11878563/how-can-i-make-this-code-more
		 * -concurrent?rq=1
		 */
		ExecutorService service = Executors.newFixedThreadPool(5);

		public UpdateSubscriptions(Context context,
				PullToRefreshListView pullToRefreshView) {
			mContext = context;
			mRefreshView = pullToRefreshView;
			subscriptionDownloader = SwipeActivity.gReader
					.getSubscriptionsFromReader();
		}

		@Override
		protected PullToRefreshListView doInBackground(Void... params) {
			try {
				Cursor subscriptionCursor = Subscription.allAsCursor(mContext
						.getContentResolver());
				while (subscriptionCursor.moveToNext()) {

					Subscription subscription = Subscription
							.getByCursor(subscriptionCursor);

					GetSubscriptionRunnable run = new GetSubscriptionRunnable(
							subscription);
					service.execute(run);

				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				mUpdateLock.release();
			}
			try {
				/*
				 * Call shutdown() before awaitTermination
				 * http://stackoverflow.com
				 * /questions/1250643/how-to-wait-for-all
				 * -threads-to-finish-using-executorservice
				 */
				service.shutdown();
				service.awaitTermination(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return mRefreshView;
		}

		protected void onPreExecute() {
			try {
				subscriptionDownloader.get(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		}

		/*
		 * For some reason this prints "Update: null" once in a while It never
		 * seems to be called with a subscription with the title null
		 * 
		 * That why I have all the checks
		 */
		@Override
		protected void onProgressUpdate(Subscription... subscription) {

			Subscription sub = subscription[0];
			CharSequence pullLabel = "Updated: " + sub.title;

			if (pullLabel != null && !pullLabel.equals("null")
					&& !pullLabel.equals("") && mRefreshView != null)
				mRefreshView.getLoadingLayoutProxy().setLastUpdatedLabel(
						pullLabel);
		}

		@Override
		protected void onPostExecute(PullToRefreshListView refreshView) {
			// Call onRefreshComplete when the list has been refreshed.
			if (mRefreshView != null)
				refreshView.onRefreshComplete();
			super.onPostExecute(refreshView);
		}

		/**
		 * A Runnable class for updating the content of a subscription.
		 * 
		 * @author Arvid Böttiger
		 */
		private class GetSubscriptionRunnable implements Runnable {
			private final Subscription subscription;

			GetSubscriptionRunnable(final Subscription subscription) {
				this.subscription = subscription;
			}

			public void run() {

				if (updateConnectStatus(mContext) == NO_CONNECT)
					return;

				if (subscription.title == null || subscription.title.equals("")
						|| subscription.title.equals("null"))
					subscription.getClass();

				FeedParserWrapper parser = new FeedParserWrapper(mContext);
				parser.parse(subscription);
				publishProgress(subscription);
			}
		}
	}
}
