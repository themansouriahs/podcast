package org.bottiger.podcast.service.Downloader;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Observable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.DownloadProgressPublisher;
import org.bottiger.podcast.model.LibraryPersistency;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.DownloadService;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.service.Downloader.engines.OkHttpDownloader;
import org.bottiger.podcast.utils.FileUtils;
import org.bottiger.podcast.utils.NetworkUtils;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.SDCardManager;
import org.bottiger.podcast.utils.StorageUtils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import rx.Observer;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import static org.bottiger.podcast.utils.StorageUtils.VIDEO;

public class SoundWavesDownloadManager extends Observable {

    private static final String TAG = "SWDownloadManager";

    public static class DownloadManagerChanged {
        public int queueSize;
        public IEpisode episode;
        public @DownloadManagerEvent int action;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OK, NO_STORAGE, OUT_OF_STORAGE, NO_CONNECTION, NEED_PERMISSION})
    public @interface Result {}
    public static final int OK = 1;
    public static final int NO_STORAGE = 2;
    public static final int OUT_OF_STORAGE = 3;
    public static final int NO_CONNECTION = 4;
    public static final int NEED_PERMISSION = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FIRST, LAST, ANYWHERE, STARTED_MANUALLY})
    public @interface QueuePosition {}
    public static final int FIRST = 1;
    public static final int LAST = 2;
    public static final int ANYWHERE = 3;
    public static final int STARTED_MANUALLY = 4;

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

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ADDED, REMOVED, CLEARED, UNDEFINED})
    public @interface DownloadManagerEvent {}
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    public static final int CLEARED = 3;
    public static final int UNDEFINED = 4;

	private Context mContext = null;

    private IDownloadEngine mEngine = null;

    private DownloadProgressPublisher mProgressPublisher;
    private IDownloadEngine.Callback mDownloadCompleteCallback;

    public SoundWavesDownloadManager(@NonNull Context argContext) {
        mContext = argContext;
        mDownloadCompleteCallback = new DownloadCompleteCallback(argContext);
        mProgressPublisher = new DownloadProgressPublisher(SoundWaves.getAppContext(argContext), this);
    }

    /**
     * Returns the status of the given FeedItem
     * @return
     */
	public DownloadStatus getStatus(IEpisode argEpisode) {
        Log.v(TAG, "getStatus(): " + argEpisode);

		if (argEpisode == null) {
            return DownloadStatus.NOTHING;
        }

        if (!(argEpisode instanceof FeedItem))
            return DownloadStatus.NOTHING;

        FeedItem item = (FeedItem)argEpisode;

        QueueEpisode qe = new QueueEpisode(item);
		if (DownloadService.getQueue().contains(qe)) {
            return DownloadStatus.PENDING;
        }

		IEpisode downloadingItem = DownloadService.getDownloadingItem();
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

    @MainThread
    public static @Result int checkPermission(TopActivity argTopActivity) {
        if (Build.VERSION.SDK_INT >= 23 &&
                (argTopActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        argTopActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED)) {

            // Should we show an explanation?
            /*
            if (activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }*/

            argTopActivity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TopActivity.PERMISSION_TO_DOWNLOAD);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant

            return NEED_PERMISSION;
        }

        return OK;
    }

    /**
	 * Download all the episodes in the queue
     *
     * @Return True if the download was started
	 */
    @WorkerThread
    @Deprecated
	private boolean startDownload(QueueEpisode nextInQueue) {
        DownloadService.download(nextInQueue.getEpisode(), nextInQueue.IsStartedManually(), mContext);
        return true;
	}

    public IDownloadEngine.Callback getIDownloadEngineCallback() {
        return mDownloadCompleteCallback;
    }

    @Deprecated
    public void removeDownloadingEpisode(IEpisode argEpisode) {
        IDownloadEngine engine = mEngine;
        if (engine != null)
            mEngine.abort();
    }

    @WorkerThread
    public IDownloadEngine newEngine(@NonNull FeedItem argEpisode) {
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

		item.delFile(context);
	}

	/**
	 * Removes all the expired downloads async
	 */
	public static void removeExpiredDownloadedPodcasts(Context context) {
        removeExpiredDownloadedPodcastsTask(context);
    }

    public static boolean removeTmpFolderCruft() {
        String tmpFolder;
        try {
            tmpFolder = SDCardManager.getTmpDir();
        } catch (IOException e) {
            Log.w(TAG, "Could not access tmp storage. removeTmpFolderCruft() returns without success"); // NoI18N
            return false;
        }
        Log.d(TAG, "Cleaning tmp folder: " + tmpFolder); // NoI18N
        File dir = new File(tmpFolder);
        if(dir.exists() && dir.isDirectory()) {
            return FileUtils.cleanDirectory(dir);
        }

        return  true;
    }

	/**
	 * Iterates through all the downloaded episodes and deletes the ones who
	 * exceed the download limit Runs with minimum priority
	 *
	 * @return Void
	 */
    @WorkerThread
    private static void removeExpiredDownloadedPodcastsTask(Context context) {

            if (BuildConfig.DEBUG && Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("Should not be executed on main thread!");
            }

			if (!SDCardManager.getSDCardStatus()) {
				return;
			}

			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);

            final long initialBytesToKeep = bytesToKeep(sharedPreferences, context.getResources());
            long bytesToKeep = initialBytesToKeep;

            // In case we do not have a limit
            if (bytesToKeep < 0) {
                return;
            }

			try {
                ArrayList<IEpisode> episodes = SoundWaves.getAppContext(context).getLibraryInstance().getEpisodes();
                LinkedList<String> filesToKeep = new LinkedList<>();

                if (episodes == null)
                    return;

                IEpisode episode;
                FeedItem item;
                File file;

                // Build list of downloaded files
                SortedMap<Long, FeedItem> sortedMap = new TreeMap<>();
                for (int i = 0; i < episodes.size(); i++) {
                    // Extract data.
                    episode = episodes.get(i);
                    try {
                        item = (FeedItem) episode;
                    } catch (ClassCastException cce) {
                        continue;
                    }

                    if (item.isDownloaded()) {
                        long key;

                        file = new File(item.getAbsolutePath());
                        key = file.lastModified();

                        sortedMap.put(-key, item);
                    }
                }

                SortedSet<Long> keys = new TreeSet<>(sortedMap.keySet());
                for (Long key : keys) {
                    boolean deleteFile = true;

                    item = sortedMap.get(key);
                    file = new File(item.getAbsolutePath());

					if (file.exists()) {
						bytesToKeep = bytesToKeep - item.filesize;

                        // if we have exceeded our limit start deleting old
						// items
						if (bytesToKeep < 0) {
							deleteExpireFile(context, item);
						} else {
							deleteFile = false;
							filesToKeep.add(item.getFilename());
						}
					}

					if (deleteFile) {
						item.setDownloaded(false);
					}
				}

				// Delete the remaining files which are not indexed in the
				// database
				// Duplicated code from DownloadManagerReceiver
				File directory = new File(SDCardManager.getDownloadDir());
				File[] files = directory.listFiles();
				for (File keepFile : files) {
					if (!filesToKeep.contains(keepFile.getName())) {
						// Delete each file
						keepFile.delete();
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
	}

    public static boolean canPerform(@Action int argAction,
                                     @NonNull Context argContext,
                                     @NonNull ISubscription argSubscription) {
        Log.v(TAG, "canPerform: " + argAction);

        @NetworkState int networkState = updateConnectStatus(argContext);

        if (networkState == NETWORK_DISCONNECTED)
            return false;

        boolean wifiOnly = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_download_only_wifi_key,
                R.bool.pref_download_only_wifi_default);

        boolean automaticDownload = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_download_on_update_key,
                R.bool.pref_download_on_update_default);

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

	private static @NetworkState int updateConnectStatus(@NonNull Context argContext) {
		Log.v(TAG, "updateConnectStatus");

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
                boolean wifiOnly = PreferenceHelper.getBooleanPreferenceValue(argContext,
                        R.string.pref_download_only_wifi_key,
                        R.bool.pref_download_only_wifi_default);

                return wifiOnly ? NETWORK_RESTRICTED : NETWORK_OK;
            }
        }

        return NETWORK_OK;
	}

	/**
	 * Add feeditem to the download queue
	 */
	public void addItemToQueue(IEpisode argEpisode, boolean startedManually, @QueuePosition int argPosition) {
        Log.d(TAG, "Adding item to queue: " + argEpisode);

        if (!(argEpisode instanceof FeedItem)) {
            return;
        }

        DownloadService.download(argEpisode, startedManually, mContext);
	}

    public int getQueueSize() {
        return DownloadService.getQueue().size();
    }

    @Nullable
    public QueueEpisode getQueueItem(int position) {
        try {
            return DownloadService.getQueue().get(position);
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    public void cancelCurrentDownload() {
        mEngine.abort();
    }

	/**
	 * Add feeditem to the download queue and start downloading at once
	 */
	public void addItemAndStartDownload(@NonNull IEpisode item, @QueuePosition int argPosition) {
        addItemToQueue(item, true, argPosition);
	}

    @Nullable
    public IDownloadEngine getCurrentDownloadProcess() {
        return mEngine;
    }

    public static long bytesToKeep(@NonNull SharedPreferences argSharedPreference, @NonNull Resources argResources) {
        String key = argResources.getString(R.string.pref_podcast_collection_size_key);
        String megabytesToKeepAsString = argSharedPreference.getString(
                key, "1000");

        long megabytesToKeep = Long.parseLong(megabytesToKeepAsString);

        if (megabytesToKeep < 0) {
            return megabytesToKeep;
        }

        long bytesToKeep = megabytesToKeep * 1024 * 1024;

        return bytesToKeep;
    }

    public class DownloadCompleteCallback implements IDownloadEngine.Callback {

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
                mimetype = StorageUtils.getMimeType(item.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            item.setIsVideo(StorageUtils.getFileType(mimetype) == VIDEO);

            SoundWaves.getAppContext(mContext).getLibraryInstance().updateEpisode(item);

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            try {
                intent.setData(Uri.fromFile(new File(item.getAbsolutePath())));
            } catch (IOException e) {
                Log.w(TAG, "Could not add file to media scanner"); // NoI18N
                e.printStackTrace();
            }


            mContext.sendBroadcast(intent);

            DownloadService.removeFirst();

            removeDownloadingEpisode(argEpisode);
            StorageUtils.removeExpiredDownloadedPodcasts(mContext);
            StorageUtils.removeTmpFolderCruft();

            notifyDownloadComplete(argEpisode);
        }

        @Override
        public void downloadInterrupted(IEpisode argEpisode) {
            removeTopQueueItem();
            removeDownloadingEpisode(argEpisode);
            StorageUtils.removeTmpFolderCruft();
            notifyDownloadComplete(argEpisode);
        }
    }

    public void notifyDownloadComplete(@Nullable IEpisode argFeedItem) {
        postQueueChangedEvent(argFeedItem, REMOVED);
    }

    @Deprecated
    private void removeTopQueueItem() {
        DownloadService.removeFirst();
    }

    private static DownloadManagerChanged produceDownloadManagerState(@Nullable IEpisode argFeedItem, @DownloadManagerEvent int eventType) {
        final DownloadManagerChanged event = new DownloadManagerChanged();
        event.episode = argFeedItem;
        event.action = eventType;
        event.queueSize = DownloadService.getSize();

        return event;
    }

    private rx.Observable<QueueEpisode> _getDownloadObservable(final QueueEpisode argQueueItem) {
        return rx.Observable.just(true).map(new Func1<Boolean, QueueEpisode>() {

            @Override
            public QueueEpisode call(Boolean argQueueItem2) {
                startDownload(argQueueItem);
                return argQueueItem;
            }
        });
    }

    @WorkerThread
    private boolean downloadEpisode(final QueueEpisode argQueueItem) {
        return startDownload(argQueueItem);
    }

    public static void postQueueChangedEvent(@Nullable IEpisode argFeedItem, @DownloadManagerEvent int eventType) {
        final DownloadManagerChanged event = produceDownloadManagerState(argFeedItem, eventType);

        Log.d(TAG, "posting DownloadManagerChanged event");
        SoundWaves.getRxBus().send(event);
        Log.d(TAG, "DownloadManagerChanged event posted");
    }
}
