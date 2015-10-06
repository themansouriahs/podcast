package org.bottiger.podcast.service.Downloader;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.DownloadProgressPublisher;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.service.Downloader.engines.OkHttpDownloader;
import org.bottiger.podcast.utils.SDCardManager;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class SoundWavesDownloadManager extends Observable {

    public static final String TAG = "EpisodeDownload";

    private static final boolean DOWNLOAD_WIFI_ONLY = false;
    private static final boolean DOWNLOAD_AUTOMATICALLY = false;

    private static final String MIME_AUDIO = "audio";
    private static final String MIME_VIDEO = "video";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OK, NO_STORAGE, OUT_OF_STORAGE, NO_CONNECTION, NEED_PERMISSION})
    public @interface Result {}
    public static final int OK = 1;
    public static final int NO_STORAGE = 2;
    public static final int OUT_OF_STORAGE = 3;
    public static final int NO_CONNECTION = 4;
    public static final int NEED_PERMISSION = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FIRST, LAST, ANYWHERE})
    public @interface QueuePosition {}
    public static final int FIRST = 1;
    public static final int LAST = 2;
    public static final int ANYWHERE = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AUDIO, VIDEO, OTHER})
    public @interface MimeType {}
    public static final int AUDIO = 1;
    public static final int VIDEO = 2;
    public static final int OTHER = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NETWORK_OK, NETWORK_RESTRICTED, NETWORK_DISCONNECTED})
    public @interface NetworkState {}
    public static final int NETWORK_OK = 1;
    public static final int NETWORK_RESTRICTED = 2;
    public static final int NETWORK_DISCONNECTED = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACTION_REFRESH_SUBSCRIPTION, ACTION_STREAM_EPISODE, ACTION_DOWNLOAD_MANUALLY, ACTION_DOWNLOAD_AUTOMATICALLY})
    public @interface Action {}
    public static final int ACTION_REFRESH_SUBSCRIPTION = 1;
    public static final int ACTION_STREAM_EPISODE = 2;
    public static final int ACTION_DOWNLOAD_MANUALLY = 3;
    public static final int ACTION_DOWNLOAD_AUTOMATICALLY = 4;

    private SharedPreferences sSharedPreferences;

	private Context mContext = null;

    private LinkedList<QueueEpisode> mDownloadQueue = new LinkedList<>();

	private IEpisode mDownloadingItem = null;
    private IDownloadEngine mEngine = null;
	private Set<Long> mDownloadingIDs = new HashSet<>();

    private DownloadProgressPublisher mProgressPublisher;
    private IDownloadEngine.Callback mDownloadCompleteCallback;

    public SoundWavesDownloadManager(@NonNull Context argContext) {
        mContext = argContext;
        mDownloadCompleteCallback = new DownloadCompleteCallback(argContext);
        mProgressPublisher = new DownloadProgressPublisher((SoundWaves)SoundWaves.getAppContext(), this);
    }

    public static @SoundWavesDownloadManager.MimeType int getFileType(@Nullable String argMimeType) {
        if (TextUtils.isEmpty(argMimeType))
            return OTHER;

        String lowerCase = argMimeType.toLowerCase();

        if (lowerCase.contains(MIME_AUDIO))
            return AUDIO;

        if (lowerCase.contains(MIME_VIDEO))
            return VIDEO;

        return OTHER;
    }

    @Deprecated
    public static boolean isVideo(String argMimeType) {
        return getFileType(argMimeType) == VIDEO;
    }

    public static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    /**
     * Returns the status of the given FeedItem
     * @return
     */
	public DownloadStatus getStatus(IEpisode argEpisode) {
        Log.d(TAG, "getStatus(): " + argEpisode);

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
	 */
	public synchronized @Result int startDownload() {

        if (Build.VERSION.SDK_INT >= 23 &&
                (mContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                 mContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED)) {

            if (!(mContext instanceof Activity))
                return NEED_PERMISSION;

            TopActivity activity = (TopActivity)mContext;

            // Should we show an explanation?
            /*
            if (activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }*/

            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                                     Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        TopActivity.PERMISSION_TO_DOWNLOAD);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant

            return NEED_PERMISSION;
        }

        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
        }

		// Make sure we have access to external storage
		if (!SDCardManager.getSDCardStatusAndCreate()) {
			return NO_STORAGE;
		}

		if (updateConnectStatus(mContext) != NETWORK_OK) {
			return NO_CONNECTION;
		}

		//downloadManager = (DownloadManager) argContext
		//		.getSystemService(Context.DOWNLOAD_SERVICE);

		if (getDownloadingItem() == null && mDownloadQueue.size() > 0) {
			QueueEpisode nextInQueue = mDownloadQueue.pollFirst(); //getNextItem();
			FeedItem downloadingItem = FeedItem.getById(mContext.getContentResolver(), nextInQueue.getId());

            mEngine = newEngine(downloadingItem);
            mEngine.addCallback(mDownloadCompleteCallback);

            mDownloadingItem = downloadingItem;

            mEngine.startDownload();

            mProgressPublisher.addEpisode(downloadingItem);
        }


        return OK;
	}

    @Deprecated
    public void removeDownloadingEpisode(IEpisode argEpisode) {
        IDownloadEngine engine = mEngine;
        if (engine != null)
            mEngine.abort();
    }

    public IDownloadEngine newEngine(@NonNull FeedItem argEpisode) {
        return new OkHttpDownloader(argEpisode, this);
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

    public static boolean removeTmpFolderCruft() {
        String tmpFolder = null;
        try {
            tmpFolder = SDCardManager.getTmpDir();
        } catch (IOException e) {
            Log.w(TAG, "Could not access tmp storage. removeTmpFolderCruft() returns without success"); // NoI18N
            return false;
        }
        Log.d(TAG, "Cleaning tmp folder: " + tmpFolder); // NoI18N
        File dir = new File(tmpFolder);
        if(dir.exists() && dir.isDirectory()) {
            try {
                FileUtils.cleanDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return  true;
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

    public static boolean canPerform(@Action int argAction,
                                     @NonNull Context argContext,
                                     @NonNull ISubscription argSubscription) {
        Log.d(TAG, "canPerform: " + argAction);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        @NetworkState int networkState = updateConnectStatus(argContext);

        if (networkState == NETWORK_DISCONNECTED)
            return false;

        Resources resources = argContext.getResources();

        String only_wifi_key = resources.getString(R.string.pref_download_only_wifi_key);
        String automatic_download_key = resources.getString(R.string.pref_download_on_update_key);

        boolean wifiOnly = prefs.getBoolean(only_wifi_key, DOWNLOAD_WIFI_ONLY);
        boolean automaticDownload = prefs.getBoolean(automatic_download_key, DOWNLOAD_AUTOMATICALLY);

        if (argSubscription instanceof Subscription) {
            Subscription subscription = (Subscription) argSubscription;

            automaticDownload = subscription.doDownloadNew(automaticDownload);
        }

        switch (argAction) {
            case ACTION_DOWNLOAD_AUTOMATICALLY: {
                if (!automaticDownload)
                    return false;

                if (wifiOnly)
                    return networkState == NETWORK_OK;
                else
                    return networkState == NETWORK_OK || networkState == NETWORK_RESTRICTED;
            }
            case ACTION_DOWNLOAD_MANUALLY:
            case ACTION_REFRESH_SUBSCRIPTION:
            case ACTION_STREAM_EPISODE: {
                return networkState == NETWORK_OK || networkState == NETWORK_RESTRICTED;
            }
        }

        VendorCrashReporter.report(TAG, "canPerform defaults to false. Action: " + argAction);
        return false; // FIXME this should never happen. Ensure we never get here
    }

	protected static @NetworkState int updateConnectStatus(@NonNull Context argContext) {
		Log.d(TAG, "updateConnectStatus");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(argContext);

        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(argContext);
        }

        ConnectivityManager cm = (ConnectivityManager) argContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return NETWORK_DISCONNECTED;
        }

        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info == null) {
            return NETWORK_DISCONNECTED;
        }

        if (!info.isConnected()) {
            return NETWORK_DISCONNECTED;
        }

        int networkType = info.getType();

        switch (networkType) {
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_VPN:
                return NETWORK_OK;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            {
                Resources resources = argContext.getResources();
                String only_wifi_key = resources.getString(R.string.pref_download_only_wifi_key);
                boolean wifiOnly = sharedPreferences.getBoolean(only_wifi_key, DOWNLOAD_WIFI_ONLY);

                return wifiOnly ? NETWORK_RESTRICTED : NETWORK_OK;
            }
        }

        return NETWORK_OK;
	}

	/**
	 * Return the FeedItem currently being downloaded
	 * 
	 * @return The downloading FeedItem
	 */
    @Nullable
	public IEpisode getDownloadingItem() {
		if (mDownloadingItem == null) {
            return null;
        }


        if (mDownloadingItem != null) {
            return mDownloadingItem;
        }

		return null;
	}

	public void notifyDownloadComplete() {
		mDownloadingItem = null;
	}

	/**
	 * @return the mDownloadingIDs
	 */
	public Set<Long> getmDownloadingIDs() {
		return mDownloadingIDs;
	}

	/**
	 * Add feeditem to the download queue
	 */
	public synchronized void addItemToQueue(IEpisode argEpisode, @QueuePosition int argPosition) {
        if (!(argEpisode instanceof FeedItem)) {
            return;
        }

		QueueEpisode queueItem = new QueueEpisode((FeedItem)argEpisode);

        if (argPosition == ANYWHERE) {
            if (!mDownloadQueue.contains(queueItem))
                mDownloadQueue.add(queueItem);

            return;
        }

        if (mDownloadQueue.contains(queueItem)) {
            mDownloadQueue.remove(queueItem);
        }

        if (argPosition == FIRST) {
            mDownloadQueue.addFirst(queueItem);
        } else if (argPosition == LAST) {
            mDownloadQueue.addLast(queueItem);
        }
	}

    /**
     * Add feeditem to the download queue
     */
    public synchronized void removeFromQueue(IEpisode argEpisode) {
        IEpisode episode;
        QueueEpisode qEpisode;
        for (int i = 0; i < mDownloadQueue.size(); i++) {
            qEpisode = mDownloadQueue.get(i);
            if (qEpisode != null) {
                episode = qEpisode.getEpisode();
                if (episode.equals(argEpisode)) {
                    mDownloadQueue.remove(argEpisode);
                }
            }

        }
    }

    public List<QueueEpisode> getQueue() {
        return mDownloadQueue;
    }

	/**
	 * Add feeditem to the download queue and start downloading at once
	 */
	public void addItemAndStartDownload(@NonNull IEpisode item, @NonNull @QueuePosition int argPosition) {
        addItemToQueue(item, argPosition);
		startDownload();
	}

    @Nullable
    public IDownloadEngine getCurrentDownloadProcess() {
        return mEngine;
    }

    public static long bytesToKeep(@NonNull SharedPreferences argSharedPreference) {
        String megabytesToKeepAsString = argSharedPreference.getString(
                "pref_podcast_collection_size", "1000");

        long megabytesToKeep = Long.parseLong(megabytesToKeepAsString);
        long bytesToKeep = megabytesToKeep * 1024 * 1024;

        return bytesToKeep;
    }

    private class DownloadCompleteCallback implements IDownloadEngine.Callback {

        @NonNull private Context mContext;

        public DownloadCompleteCallback(@NonNull Context argContext) {
            mContext = argContext;
        }

        @Override
        public void downloadCompleted(IEpisode argEpisode) {
            FeedItem item = (FeedItem) argEpisode;
            item.setDownloaded(true);

            String mimetype = null;
            try {
                mimetype = getMimeType(item.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            item.setIsVideo(getFileType(mimetype) == VIDEO);

            item.update(mContext.getContentResolver());

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            try {
                intent.setData(Uri.fromFile(new File(item.getAbsolutePath())));
            } catch (IOException e) {
                Log.w(TAG, "Could not add file to media scanner"); // NoI18N
                e.printStackTrace();
            }


            mContext.sendBroadcast(intent);

            Playlist.refresh(mContext);

            removeDownloadingEpisode(argEpisode);
            removeExpiredDownloadedPodcasts(mContext);
            removeTmpFolderCruft();

            // clear the reference
            item = null;
            notifyDownloadComplete();

            startDownload();
        }

        @Override
        public void downloadInterrupted(IEpisode argEpisode) {
            removeDownloadingEpisode(argEpisode);
            removeTmpFolderCruft();
            startDownload();
        }
    }
}
