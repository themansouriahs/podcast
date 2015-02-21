package org.bottiger.podcast.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Observable;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.images.RequestManager;
import org.bottiger.podcast.listeners.DownloadFileObserver;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.parser.JSONFeedParserWrapper;
import org.bottiger.podcast.parser.syndication.handler.FeedHandler;
import org.bottiger.podcast.parser.syndication.handler.UnsupportedFeedtypeException;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.IDownloadEngine;
import org.bottiger.podcast.service.Downloader.OkHttpDownloader;
import org.bottiger.podcast.utils.LockHandler;
import org.bottiger.podcast.utils.SDCardManager;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.FileObserver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public class PodcastDownloadManager extends Observable {

    public static boolean isDownloading = false;

	// Running processes
	private static AtomicInteger processCounts = new AtomicInteger(0);
	private static Context mContext = null;

	public static final int NO_CONNECT = 1;
	public static final int WIFI_CONNECT = 2;
	public static final int MOBILE_CONNECT = 4;

	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;

	private static long pref_update = 2 * 60 * ONE_MINUTE;

    private static HashMap<Long, DownloadFileObserver> mFileObserver = new HashMap<Long, DownloadFileObserver>();

	private static PriorityQueue<QueueEpisode> mDownloadQueue = new PriorityQueue<QueueEpisode>();

	public static FeedItem mDownloadingItem = null;
	private static HashSet<Long> mDownloadingIDs = new HashSet<Long>();
    public static HashMap<Long, IDownloadEngine> mDownloadingEpisodes = new HashMap<Long, IDownloadEngine>();

	private static final LockHandler mUpdateLock = new LockHandler();
	private static int mConnectStatus = NO_CONNECT;

	public static long pref_update_wifi = 0;
	public static long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 1000;
	public long pref_played_file_expire = 0;
	public int pref_max_valid_size = 20;

	private static DownloadManager downloadManager;
	private static long downloadReference;

    /**
     * Returns the status of the given FeedItem
     * @param item
     * @return
     */
	public static DownloadStatus getStatus(FeedItem item) {
		if (item == null) {
            return DownloadStatus.NOTHING;
        }

		if (mDownloadQueue.contains(item)) {
            return DownloadStatus.PENDING;
        }

		FeedItem downloadingItem = getDownloadingItem();
		if (downloadingItem != null) {
            if (item.equals(downloadingItem)) {
                return DownloadStatus.DOWNLOADING;
            }
        }

		if (item.isDownloaded()) {
			return DownloadStatus.DONE;
		} else if (item.chunkFilesize > 0) {
			return DownloadStatus.ERROR;
			// consider deleting it here
		}

		return DownloadStatus.NOTHING;
	}

	public static void start_update(final Context context) {
		start_update(context, null, null);
	}

	public static void start_update(final Context context,
			Subscription subscription, DownloadCompleteCallback argCallback) {
		if (updateConnectStatus(context) == NO_CONNECT)
			return;

        isDownloading = true;

		mContext = context;

		// FIXME
		// Perhaps we should do this in the background in the future
		RequestManager.initIfNeeded(context);
		RequestQueue requestQueue = RequestManager.getRequestQueue();

		Cursor subscriptionCursor;

        if (subscription == null) {
            subscriptionCursor = Subscription.allAsCursor(context
                    .getContentResolver());

            while (subscriptionCursor.moveToNext()) {

                Subscription sub = Subscription.getByCursor(subscriptionCursor);

                if (subscription == null || sub.equals(subscription)) {

                    addSubscriptionToQueue(sub, requestQueue, argCallback);
                    /*
                    StringRequest jr = new StringRequest(sub.getUrl(),
                            new MyStringResponseListener(context
                                    .getContentResolver(), argCallback, sub),
                            createGetFailureListener());

                    int MY_SOCKET_TIMEOUT_MS = 300000;
                    DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                            MY_SOCKET_TIMEOUT_MS,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                    jr.setRetryPolicy(retryPolicy);

                    // Add the request to Volley
                    requestQueue.add(jr);
                    processCounts.incrementAndGet();
                    */

                }
            }
        } else {
            addSubscriptionToQueue(subscription, requestQueue, argCallback);
        }

		requestQueue.start();
	}

    private static void addSubscriptionToQueue(Subscription argSubscription, RequestQueue requestQueue, DownloadCompleteCallback argCallback) {
        StringRequest jr = new StringRequest(argSubscription.getUrl(),
                new MyStringResponseListener(mContext
                        .getContentResolver(), argCallback, argSubscription),
                createGetFailureListener());

        int MY_SOCKET_TIMEOUT_MS = 300000;
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jr.setRetryPolicy(retryPolicy);

        // Add the request to Volley
        requestQueue.add(jr);
        processCounts.incrementAndGet();
    }

	static class MyStringResponseListener implements Listener<String> {

		static FeedHandler feedHandler = new FeedHandler();
		Subscription subscription;
		ContentResolver contentResolver;
		final JSONFeedParserWrapper feedParser = null;
        DownloadCompleteCallback callback;

		public MyStringResponseListener(ContentResolver contentResolver,
                                        DownloadCompleteCallback argCallback, Subscription subscription) {
			this.subscription = subscription;
			this.contentResolver = contentResolver;
            this.callback = argCallback;
		}

		@Override
		public void onResponse(String response) {
			// volleyResultParser.execute(response);

            new ParseFeedTask().execute(response);
			//decrementProcessCount();
		}

        private class ParseFeedTask extends AsyncTask<String, Void, Void> {
            protected Void doInBackground(String... responses) {

                Subscription sub = null;
                String response = responses[0];

                try {
                    sub = feedHandler.parseFeed(contentResolver, subscription,
                            response.replace("ï»¿", "")); // Byte Order Mark
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (UnsupportedFeedtypeException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return null;
            }

            protected void onPostExecute(Void result) {
                decrementProcessCount();
                if (callback != null) {
                    callback.complete(true);
                }
            }
        }


    }

	private static Response.ErrorListener createGetFailureListener() {
		return new Response.ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) { // Handle error
				decrementProcessCount();
				if (error instanceof com.android.volley.ServerError) {

				} else {
					error.printStackTrace();
					int i = 5;
					i = i + i;
				}
			}
		};
	}

	private static void decrementProcessCount() {
		PodcastDownloadManager.processCounts.decrementAndGet();
		if (PodcastDownloadManager.processCounts.get() == 0) {
            isDownloading = false;
			if (mContext != null) {
                PodcastDownloadManager.startDownload(mContext);
            }
		}
	}

	/**
	 * Download all the episodes in the queue
	 * 
	 * @param show
	 * @param context
	 */
	@SuppressLint("NewApi")
	public static synchronized void startDownload(final Context context) {

        if (mContext == null) {
            mContext = context;
        }

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);

		// Make sure we have access to external storage
		if (SDCardManager.getSDCardStatusAndCreate() == false) {
			return;
		}

		if (updateConnectStatus(context) == NO_CONNECT) {
			return;
		}

		downloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);

		Playlist playlist = PlayerService.getPlaylist();
        playlist.populatePlaylistIfEmpty();

		int max = playlist.size() > 5 ? 5 : playlist.size();
		for (int i = 0; i < max; i++) {
			FeedItem item = playlist.getItem(i);
			if (item != null && !item.isDownloaded())
				mDownloadQueue.add(new QueueEpisode(item));
		}

		if (getDownloadingItem() == null && mDownloadQueue.size() > 0) {
			QueueEpisode nextInQueue = getNextItem();
			FeedItem downloadingItem = FeedItem.getById(context.getContentResolver(), nextInQueue.getId());

            /*
            Uri downloadURI = Uri.parse(downloadingItem.url);

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
			request.setTitle(downloadingItem.title);
			// Set a description of this download, to be displayed in
			// notifications
			// (if enabled)
			request.setDescription(downloadingItem.content);
			// Set the local destination for the downloaded file to a path
			// within
			// the application's external files directory
			// String downloadDir = SDCardManager.getDownloadDir();
			// String fileName = mDownloadingItem.getFilename();
			// request.setDestinationInExternalFilesDir(context,
			// downloadDir, fileName);
			File file = new File(downloadingItem.getAbsoluteTmpPath());
			request.setDestinationUri(Uri.fromFile(file));

			// Enqueue a new download and same the referenceId
			downloadReference = downloadManager.enqueue(request);
			PodcastDownloadManager.mDownloadingIDs.add(downloadReference);

			downloadingItem.setDownloadReferenceID(downloadReference);
			downloadingItem.update(context.getContentResolver());
			mDownloadingItem = downloadingItem;
			*/
            IDownloadEngine downloadEngine = newEngine(downloadingItem);
            downloadEngine.addCallback(new IDownloadEngine.Callback() {
                @Override
                public void downloadCompleted(long argID) {
                    FeedItem item = mDownloadingEpisodes.get(argID).getEpisode();
                    item.setDownloaded(true);
                    item.update(mContext.getContentResolver());

                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(new File(item.getAbsolutePath())));
                    context.sendBroadcast(intent);

                    removeDownloadingEpisode(argID);
                }

                @Override
                public void downloadInterrupted(long argID) {
                    removeDownloadingEpisode(argID);
                }
            });

            downloadEngine.startDownload();
            mDownloadingEpisodes.put(new Long(downloadingItem.getId()), downloadEngine);

            getDownloadProgressObservable(mContext).addEpisode(downloadingItem.getId());
        }

	}

    public static void removeDownloadingEpisode(long argID) {
        if (mDownloadingEpisodes.containsKey(argID)) {
            mDownloadingEpisodes.remove(argID);
        } else {
            throw new IllegalStateException("No such epiode in download list");
        }
    }

    public static IDownloadEngine newEngine(@NonNull FeedItem argEpisode) {
        return new OkHttpDownloader(argEpisode);
    }

    public static HashMap<Long, DownloadFileObserver> getFileObservers() {
        return mFileObserver;
    }

	/**
	 * Deletes the downloaded file and updates the database record
	 * 
	 * @param context
	 * @param cursor
	 */
	private static void deleteExpireFile(Context context, FeedItem item) {

		if (item == null)
			return;

		ContentResolver contentResolver = context.getContentResolver();
		item.delFile(contentResolver);
	}

	/**
	 * Removes all the expired downloads async
	 */
	public static void removeExpiredDownloadedPodcasts(Context context) {
		new removeExpiredDownloadedPodcastsTask(context).execute();
	}

	/**
	 * Iterates through all the downloaded episodes and deletes the ones who
	 * exceed the download limit Runs with minimum priority
	 * 
	 * @param context
	 * @return Void
	 */
	private static class removeExpiredDownloadedPodcastsTask extends
			AsyncTask<Void, Integer, Void> {

		Context mContext = null;

		public removeExpiredDownloadedPodcastsTask(Context context) {
			mContext = context;
		}

		// Do the long-running work in here
		@Override
		protected Void doInBackground(Void... params) {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

			if (SDCardManager.getSDCardStatus() == false) {
				return null;
			}

			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(mContext);

			String megabytesToKeepAsString = sharedPreferences.getString(
					"pref_podcast_collection_size", "1000");

			long megabytesToKeep = Long.parseLong(megabytesToKeepAsString);
			long bytesToKeep = megabytesToKeep * 1024 * 1024;

			try {
				// Fetch all downloaded podcasts
				String where = ItemColumns.IS_DOWNLOADED + "==1";

				// sort by nevest first
				String sortOrder = ItemColumns.LAST_UPDATE + " DESC";

				Cursor cursor = mContext.getContentResolver().query(
						ItemColumns.URI, ItemColumns.ALL_COLUMNS, where, null,
						sortOrder);

				LinkedList<String> filesToKeep = new LinkedList<String>();
				cursor.moveToFirst();
				while (cursor.isAfterLast() == false) {
					boolean deleteFile = true;
					// Extract data.
					FeedItem item = FeedItem.getByCursor(cursor);
					if (item != null) {
						File file = new File(item.getAbsolutePath());

						if (file.exists()) {
							bytesToKeep = bytesToKeep - item.filesize;

							// if we have exceeded our limit start deleting old
							// items
							if (bytesToKeep < 0) {
								deleteExpireFile(mContext, item);
							} else {
								deleteFile = false;
								filesToKeep.add(item.getFilename());
							}
						}

						if (deleteFile) {
							item.setDownloaded(false);
							item.update(mContext.getContentResolver());
						}
						cursor.moveToNext();
					}
				}

				// Delete the remaining files which are not indexed in the
				// database
				// Duplicated code from DownloadManagerReceiver
				File directory = new File(SDCardManager.getDownloadDir());
				File[] files = directory.listFiles();
				for (File file : files) {
					if (!filesToKeep.contains(file.getName())) {
						// Delete each file
						file.delete();
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;

		}
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

	/**
	 * Return the FeedItem currently being downloaded
	 * 
	 * @return The downloading FeedItem
	 */
	public static FeedItem getDownloadingItem() {
		if (mDownloadingItem == null) {
            return null;
        }

        if (mDownloadingItem != null) {
            return mDownloadingItem;
        }

		long downloadReference = mDownloadingItem.getDownloadReferenceID();
		Query query = new Query();
		query.setFilterById(downloadReference);
		Cursor c = downloadManager.query(query);
		if (c != null && c.moveToFirst()) {
			int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int status = c.getInt(columnIndex);

			if (status == DownloadManager.STATUS_RUNNING)
				return mDownloadingItem;
		}

		return null;
	}

	public static void notifyDownloadComplete(FeedItem completedItem) {
		assert completedItem != null;
		if (completedItem.equals(mDownloadingItem))
			mDownloadingItem = null;
	}

	private static QueueEpisode getNextItem() {
		return mDownloadQueue.poll();
	}

	/**
	 * @return the mDownloadingIDs
	 */
	public static HashSet<Long> getmDownloadingIDs() {
		return mDownloadingIDs;
	}

	/**
	 * Add feeditem to the download queue
	 * 
	 * @param feedItem
	 */
	public static void addItemToQueue(FeedItem item) {
		QueueEpisode queueItem = new QueueEpisode(item);
		if (!mDownloadQueue.contains(queueItem))
			mDownloadQueue.add(queueItem);
	}
	
	/**
	 * Replace item in the queue
	 */
	public static void replace(QueueEpisode episode) {
		if (mDownloadQueue.remove(episode)) {
			mDownloadQueue.add(episode);
		}
	}
	
	public static PriorityQueue<QueueEpisode> getQueue() {
		return mDownloadQueue;
	}

	/**
	 * Cancel all current downloads
	 */
	public static void cancelAllDownloads(Context context) {

		downloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);

		Query query = new Query();
		query.setFilterByStatus(DownloadManager.STATUS_RUNNING
				| DownloadManager.STATUS_PENDING
				| DownloadManager.STATUS_FAILED | DownloadManager.STATUS_PAUSED
				| DownloadManager.STATUS_SUCCESSFUL);
		Cursor cursor = downloadManager.query(query);

		int counter = 0;

		if (cursor.moveToFirst()) {
			do {
				counter++;
				int cursorIndex = cursor
						.getColumnIndex(DownloadManager.COLUMN_ID);
				Long downloadID = cursor.getLong(cursorIndex);

				downloadManager.remove(downloadID);

			} while (cursor.moveToNext());
		}

		counter = counter + 1;
	}

	/**
	 * Add feeditem to the download queue and start downloading at once
	 * 
	 * @param feedItem
	 * @param context
	 */
	public static void addItemAndStartDownload(FeedItem item, Context context) {
		mDownloadQueue.add(new QueueEpisode(item));
		startDownload(context);
	}


    private static DownloadProgressObservable mDownloadProgressObservable;

    public static DownloadProgressObservable getDownloadProgressObservable(Context context) {
        if (mDownloadProgressObservable == null) {
            mDownloadProgressObservable = new DownloadProgressObservable(context);
        }
        return mDownloadProgressObservable;
    }
    /**
     *
     * @param argDownloadProgressObservable
     */
    public static void addDownloadProgressObservable(DownloadProgressObservable argDownloadProgressObservable) {
        if (mDownloadProgressObservable != null) {
            throw new IllegalStateException("mDownloadProgressObservable must be null");
        }

        mDownloadProgressObservable = argDownloadProgressObservable;
    }

    public static void resetDownloadProgressObservable() {
        mDownloadProgressObservable = null;
    }

	/**
	 * Update the list of subscriptions as well as their content
	 * 
	 * @author Arvid Böttiger
	 */
	private static class UpdateSubscriptions extends
			AsyncTask<Void, Subscription, Void> {
		private Context mContext;
		private Subscription mSubscription;
		//private PullToRefreshListView mRefreshView;
		private AsyncTask<URL, Void, Void> subscriptionDownloader;

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
				Void pullToRefreshView,
				Subscription subscription) {
			mContext = context;
			mSubscription = subscription;
			//mRefreshView = pullToRefreshView;
			subscriptionDownloader = null;

			if (MainActivity.gReader != null) {
				subscriptionDownloader = MainActivity.gReader
						.getSubscriptionsFromReader();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			try {

				if (mSubscription != null) {
					GetSubscriptionRunnable run = new GetSubscriptionRunnable(
							mSubscription);
					service.execute(run);
				} else {
					Cursor subscriptionCursor = Subscription
							.allAsCursor(mContext.getContentResolver());

					while (subscriptionCursor.moveToNext()) {

						Subscription subscription = Subscription
								.getByCursor(subscriptionCursor);

						GetSubscriptionRunnable run = new GetSubscriptionRunnable(
								subscription);
						service.execute(run);
					}
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

			//return mRefreshView;
            return null;
		}

		@Override
		protected void onPreExecute() {
			try {
				if (subscriptionDownloader != null)
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

            /*
			if (pullLabel != null && !pullLabel.equals("null")
					&& !pullLabel.equals("") && mRefreshView != null)
				mRefreshView.getLoadingLayoutProxy().setLastUpdatedLabel(
						pullLabel);
						*/
		}

        /*
		@Override
		protected void onPostExecute(PullToRefreshListView refreshView) {
			// Call onRefreshComplete when the list has been refreshed.
			if (mRefreshView != null)
				refreshView.onRefreshComplete();
			super.onPostExecute(refreshView);
		}
		*/

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

			@Override
			public void run() {

				if (updateConnectStatus(mContext) == NO_CONNECT)
					return;

				if (subscription.title == null || subscription.title.equals("")
						|| subscription.title.equals("null"))
					subscription.getClass();

				JSONFeedParserWrapper parser = new JSONFeedParserWrapper(
						mContext);
				publishProgress(subscription);
				// parser.parse(subscription);
			}
		}
	}
}
