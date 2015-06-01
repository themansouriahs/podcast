package org.bottiger.podcast.service.Downloader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.PlaylistData;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.service.Downloader.engines.OkHttpDownloader;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.SDCardManager;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

public class EpisodeDownloadManager extends Observable {

    public static final String DEBUG_KEY = "EpisodeDownload";
    private static final boolean DOWNLOAD_WIFI_ONLY = false;
    private static final boolean DOWNLOAD_AUTOMATICALLY = false;

    public static boolean isDownloading = false;

    public enum RESULT { OK, NO_STORAGE, OUT_OF_STORAGE, NO_CONNECTION }
    public enum QUEUE_POSITION { FIRST, LAST, ANYWHERE}
    public enum NETWORK_STATE { OK, RESTRICTED, DISCONNECTED }
	public enum ACTION { REFRESH_SUBSCRIPTION, STREAM_EPISODE, DOWNLOAD_MANUALLY, DOWNLOAD_AUTOMATICALLY }

    private static SharedPreferences sSharedPreferences;

	private static Context mContext = null;

    private static LinkedList<QueueEpisode> mDownloadQueue = new LinkedList<>();

	public static IEpisode mDownloadingItem = null;
	private static HashSet<Long> mDownloadingIDs = new HashSet<>();
    public static HashMap<IEpisode, IDownloadEngine> mDownloadingEpisodes = new HashMap<>();

	private static DownloadManager downloadManager;

    private static IDownloadEngine.Callback mDownloadCompleteCallback = new IDownloadEngine.Callback() {
        @Override
        public void downloadCompleted(IEpisode argEpisode) {
            FeedItem item = (FeedItem) argEpisode;
            item.setDownloaded(true);
            item.update(mContext.getContentResolver());

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(new File(item.getAbsolutePath())));
            mContext.sendBroadcast(intent);

            Playlist.refresh(mContext);

            removeDownloadingEpisode(argEpisode);
            removeExpiredDownloadedPodcasts(mContext);

            startDownload(mContext);
        }

        @Override
        public void downloadInterrupted(IEpisode argEpisode) {
            removeDownloadingEpisode(argEpisode);
            startDownload(mContext);
        }
    };

    /**
     * Returns the status of the given FeedItem
     * @return
     */
	public static DownloadStatus getStatus(IEpisode argEpisode) {
        Log.d(DEBUG_KEY, "getStatus(): " + argEpisode);

		if (argEpisode == null) {
            return DownloadStatus.NOTHING;
        }

        if (!(argEpisode instanceof FeedItem))
            return DownloadStatus.NOTHING;

        FeedItem item = (FeedItem)argEpisode;

        QueueEpisode qe = new QueueEpisode(item);
		if (mDownloadQueue.contains(qe)) {
            return DownloadStatus.PENDING;
        }

		IEpisode downloadingItem = getDownloadingItem();
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

	/**
	 * Download all the episodes in the queue
	 *
	 * @param argContext
	 */
	public static synchronized RESULT startDownload(@NonNull final Context argContext) {

        if (mContext == null) {
            mContext = argContext;
        }

        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(argContext);
        }

		// Make sure we have access to external storage
		if (!SDCardManager.getSDCardStatusAndCreate()) {
			return RESULT.NO_STORAGE;
		}

		if (updateConnectStatus(argContext) != NETWORK_STATE.OK) {
			return RESULT.NO_CONNECTION;
		}

		downloadManager = (DownloadManager) argContext
				.getSystemService(Context.DOWNLOAD_SERVICE);

		if (getDownloadingItem() == null && mDownloadQueue.size() > 0) {
			QueueEpisode nextInQueue = mDownloadQueue.pollFirst(); //getNextItem();
			FeedItem downloadingItem = FeedItem.getById(argContext.getContentResolver(), nextInQueue.getId());

            /*
            Uri downloadURI = Uri.parse(downloadingItem.url);

			DownloadManager.Request request = new DownloadManager.Request(
					downloadURI);

			// Restrict the types of networks over which this download may
			// proceed.
			int networkType = DownloadManager.Request.NETWORK_WIFI;

			// Only Allow mobile network if the user has enabled it
			if (!sSharedPreferences.getBoolean("pref_download_only_wifi", true))
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
            downloadEngine.addCallback(mDownloadCompleteCallback);

            downloadEngine.startDownload();
            mDownloadingEpisodes.put(downloadingItem, downloadEngine);

            getDownloadProgressObservable((SoundWaves)mContext.getApplicationContext()).addEpisode(downloadingItem);
        }


        return RESULT.OK;
	}

    public static void removeDownloadingEpisode(IEpisode argEpisode) {
        if (mDownloadingEpisodes.containsKey(argEpisode)) {
            mDownloadingEpisodes.remove(argEpisode);
        } else {
            throw new IllegalStateException("No such epiode in download list");
        }
    }

    public static IDownloadEngine newEngine(@NonNull FeedItem argEpisode) {
        return new OkHttpDownloader(argEpisode);
    }

	/**
	 * Deletes the downloaded file and updates the database record
	 * 
	 * @param context
	 */
	private static void deleteExpireFile(@NonNull Context context, FeedItem item) {

		if (item == null)
			return;

		ContentResolver contentResolver = context.getContentResolver();
		item.delFile(context);
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

            long bytesToKeep = bytesToKeep(sharedPreferences);

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

    public static boolean canPerform(ACTION argAction, @NonNull Context argContext) {
        Log.d(DEBUG_KEY, "canPerform: " + argAction);

        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(argContext);
        }

        NETWORK_STATE networkState = updateConnectStatus(argContext);

        if (networkState == NETWORK_STATE.DISCONNECTED)
            return false;

        Resources resources = argContext.getResources();

        String only_wifi_key = resources.getString(R.string.pref_download_only_wifi_key);
        String automatic_download_key = resources.getString(R.string.pref_download_on_update_key);

        boolean wifiOnly = sSharedPreferences.getBoolean(only_wifi_key, DOWNLOAD_WIFI_ONLY);
        boolean automaticDownload = sSharedPreferences.getBoolean(automatic_download_key, DOWNLOAD_AUTOMATICALLY);

        if (argAction == ACTION.DOWNLOAD_AUTOMATICALLY) {
            if (!automaticDownload)
                return false;

            if (wifiOnly)
                return networkState == NETWORK_STATE.OK;
            else
                return networkState == NETWORK_STATE.OK || networkState == NETWORK_STATE.RESTRICTED;
        }

        if (    argAction == ACTION.STREAM_EPISODE ||
                argAction == ACTION.REFRESH_SUBSCRIPTION ||
                argAction == ACTION.DOWNLOAD_MANUALLY) {
            return networkState == NETWORK_STATE.OK || networkState == NETWORK_STATE.RESTRICTED;
        }

        VendorCrashReporter.report(DEBUG_KEY, "canPerform defaults to false. Action: " + argAction);
        return false; // FIXME this should never happen. Ensure we never get here
    }

	protected static NETWORK_STATE updateConnectStatus(@NonNull Context argContext) {
		Log.d(DEBUG_KEY, "updateConnectStatus");

        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(argContext);
        }

        ConnectivityManager cm = (ConnectivityManager) argContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return NETWORK_STATE.DISCONNECTED;
        }

        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info == null) {
            return NETWORK_STATE.DISCONNECTED;
        }

        if (!info.isConnected()) {
            return NETWORK_STATE.DISCONNECTED;
        }

        int networkType = info.getType();

        switch (networkType) {
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_VPN:
                return NETWORK_STATE.OK;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            {
                Resources resources = argContext.getResources();
                String only_wifi_key = resources.getString(R.string.pref_download_only_wifi_key);
                boolean wifiOnly = sSharedPreferences.getBoolean(only_wifi_key, DOWNLOAD_WIFI_ONLY);

                return wifiOnly ? NETWORK_STATE.RESTRICTED : NETWORK_STATE.OK;
            }
        }

        return NETWORK_STATE.OK;
	}

	/**
	 * Return the FeedItem currently being downloaded
	 * 
	 * @return The downloading FeedItem
	 */
	public static IEpisode getDownloadingItem() {
		if (mDownloadingItem == null) {
            return null;
        }


        if (mDownloadingItem != null) {
            return mDownloadingItem;
        }

        /*
		long downloadReference = mDownloadingItem.getDownloadReferenceID();
		Query query = new Query();
		query.setFilterById(downloadReference);
		Cursor c = downloadManager.query(query);
		if (c != null && c.moveToFirst()) {
			int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int status = c.getInt(columnIndex);

			if (status == DownloadManager.STATUS_RUNNING)
				return mDownloadingItem;
		}*/

		return null;
	}

	public static void notifyDownloadComplete(FeedItem completedItem) {
		assert completedItem != null;
		if (completedItem.equals(mDownloadingItem))
			mDownloadingItem = null;
	}

	/**
	 * @return the mDownloadingIDs
	 */
	public static HashSet<Long> getmDownloadingIDs() {
		return mDownloadingIDs;
	}

	/**
	 * Add feeditem to the download queue
	 */
	public static synchronized void addItemToQueue(IEpisode argEpisode, QUEUE_POSITION argPosition) {
        if (!(argEpisode instanceof FeedItem)) {
            return;
        }

		QueueEpisode queueItem = new QueueEpisode((FeedItem)argEpisode);

        if (argPosition == QUEUE_POSITION.ANYWHERE) {
            if (!mDownloadQueue.contains(queueItem))
                mDownloadQueue.add(queueItem);

            return;
        }

        if (mDownloadQueue.contains(queueItem)) {
            mDownloadQueue.remove(queueItem);
        }

        if (argPosition == QUEUE_POSITION.FIRST) {
            mDownloadQueue.addFirst(queueItem);
        } else if (argPosition == QUEUE_POSITION.LAST) {
            mDownloadQueue.addLast(queueItem);
        }
	}

	/**
	 * Add feeditem to the download queue and start downloading at once
	 *
	 * @param context
	 */
	public static void addItemAndStartDownload(@NonNull IEpisode item, @NonNull QUEUE_POSITION argPosition, @NonNull Context context) {
        addItemToQueue(item, argPosition);
		startDownload(context);
	}


    private static DownloadProgressObservable mDownloadProgressObservable;

    public static DownloadProgressObservable getDownloadProgressObservable(SoundWaves context) {
        if (mDownloadProgressObservable == null) {
            mDownloadProgressObservable = new DownloadProgressObservable(context);
        }
        return mDownloadProgressObservable;
    }


    public static void resetDownloadProgressObservable() {
        mDownloadProgressObservable = null;
    }

    public static long bytesToKeep(@NonNull SharedPreferences argSharedPreference) {
        String megabytesToKeepAsString = argSharedPreference.getString(
                "pref_podcast_collection_size", "1000");

        long megabytesToKeep = Long.parseLong(megabytesToKeepAsString);
        long bytesToKeep = megabytesToKeep * 1024 * 1024;

        return bytesToKeep;
    }
}
