package info.bottiger.podcast.service;

import info.bottiger.podcast.R;
import info.bottiger.podcast.SwipeActivity;
import info.bottiger.podcast.fetcher.FeedFetcher;
import info.bottiger.podcast.parser.FeedHandler;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.provider.SubscriptionColumns;
import info.bottiger.podcast.utils.LockHandler;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.SDCardManager;

import java.net.URL;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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
	private long pref_update = 2 * 60 * ONE_MINUTE;

	private static PriorityQueue<FeedItem> mDownloadQueue = new PriorityQueue<FeedItem>();
	private final Log log = Log.getLog(getClass());

	private static FeedItem mDownloadingItem = null;
	private static final LockHandler mDownloadLock = new LockHandler();

	private static final LockHandler mUpdateLock = new LockHandler();
	private static int mConnectStatus = NO_CONNECT;

	public long pref_update_wifi = 0;
	public long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 1000;
	public long pref_played_file_expire = 0;
	public int pref_max_valid_size = 20;

	public enum DownloadStatus {
		NOTHING, PENDING, DOWNLOADING, DONE, ERROR
	}

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

	public void start_update(final Context context) {
		start_update(context, null);
	}

	public void start_update(final Context context,
			final PullToRefreshListView pullToRefreshView) {
		if (updateConnectStatus(context) == NO_CONNECT)
			return;

		log.debug("start_update()");
		if (mUpdateLock.locked() == false)
			return;

		new UpdateSubscriptions(context, pullToRefreshView).execute();
	}

	public void do_download(boolean show, final Context context) {
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

		if (mDownloadLock.locked() == false) {
			int i = 5;
			i = i + 6;
			return;
		}

		new DownloadPodcast(context).execute();
	}

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

	private int updateConnectStatus(Context context) {
		log.debug("updateConnectStatus");
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

	public FeedItem getDownloadingItem() {
		return mDownloadingItem;
	}

	private FeedItem getNextItem() {
		return mDownloadQueue.poll();
	}

	public static void addItemToQueue(FeedItem item) {
		mDownloadQueue.add(item);

		// should we start downloading now?
	}

	private class UpdateSubscriptions extends
			AsyncTask<Void, String, PullToRefreshListView> {
		Context mContext;
		PullToRefreshListView mRefreshView;
		AsyncTask<URL, Void, Void> subscriptionDownloader;

		/*
		 *  AsyncTask may be asynchronous, but not very concurrent.
		 *  Instead of spawning a bunch of AsyncTasks to refresh our feeds we use a ThreadPool 
		 *  
		 *  http://stackoverflow.com/questions/11878563/how-can-i-make-this-code-more-concurrent?rq=1
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

					GetSubscriptionRunnable run = new GetSubscriptionRunnable(subscription);
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
				 * http://stackoverflow.com/questions/1250643/how-to-wait-for-all-threads-to-finish-using-executorservice
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
			int test = 5;
			test = test;
		}

		@Override
		protected void onProgressUpdate(String... title) {
			// Toast.makeText(mContext, "Updating: " + title[0],
			// Toast.LENGTH_LONG).show();
			CharSequence pullLabel = "Updated: " + title[0];
			if (pullLabel != null && mRefreshView != null)
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

		private class GetSubscriptionRunnable implements Runnable {
			private final Subscription subscription;

			GetSubscriptionRunnable(final Subscription message) {
				this.subscription = message;
			}

			public void run() {
				int add_num;

				if (updateConnectStatus(mContext) == NO_CONNECT)
					return;

				FeedHandler handler = new FeedHandler(
						mContext.getContentResolver(), pref_max_valid_size);
				
				add_num = handler.update(subscription);
				if ((add_num > 0) && (subscription.auto_download > 0))
					do_download(false, mContext);

				publishProgress(subscription.title);
				//message = findSubscription(mContext);
			}
		}
	}

	private class DownloadPodcast extends AsyncTask<Void, String, Void> {
		Context mContext;

		public DownloadPodcast(Context context) {
			mContext = context;
		}

		@Override
		protected Void doInBackground(Void... params) {
			int gg = 6;
			gg = gg +6;
			try {
				while ((updateConnectStatus(mContext) & pref_connection_sel) > 0) {

					mDownloadingItem = getNextItem();

					if (mDownloadingItem == null) {
						break;
					}

					try {
						// mDownloadingItem.startDownload(getContentResolver());
						FeedFetcher fetcher = new FeedFetcher();
						fetcher.download(mDownloadingItem,
								mContext.getContentResolver());

					} catch (Exception e) {
						e.printStackTrace();
					}

					log.debug(mDownloadingItem.title + "  "
							+ mDownloadingItem.length + "  "
							+ mDownloadingItem.offset);

					mDownloadingItem.endDownload(mContext.getContentResolver());

				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				mDownloadingItem = null;
				mDownloadLock.release();
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(String... title) {
			// Toast.makeText(mContext, "Updating: " + title[0],
			// Toast.LENGTH_LONG).show();
		}
	}
}
