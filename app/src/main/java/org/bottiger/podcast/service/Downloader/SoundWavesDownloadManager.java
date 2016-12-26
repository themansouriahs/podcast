package org.bottiger.podcast.service.Downloader;

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
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.support.v7.util.SortedList;
import android.util.Log;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.DownloadProgressPublisher;
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
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.SDCardManager;
import org.bottiger.podcast.utils.StorageUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.bottiger.podcast.utils.StorageUtils.VIDEO;

public class SoundWavesDownloadManager extends Observable {

    public static final int HOURS = 48;
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

    @NonNull private DownloadProgressPublisher mProgressPublisher;
    @NonNull private IDownloadEngine.Callback mDownloadCompleteCallback;

    public SoundWavesDownloadManager(@NonNull Context argContext) {
        mContext = argContext;
        mDownloadCompleteCallback = new DownloadCompleteCallback(argContext);
        mProgressPublisher = new DownloadProgressPublisher(SoundWaves.getAppContext(argContext), this);
    }

    /**
     * Returns the status of the given FeedItem
     * @return
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public DownloadStatus getStatus(IEpisode argEpisode) throws SecurityException {
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

		if (item.isDownloaded(mContext)) {
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

    @NonNull
    public IDownloadEngine.Callback getIDownloadEngineCallback() {
        return mDownloadCompleteCallback;
    }

    @Deprecated
    public void removeDownloadingEpisode(IEpisode argEpisode) {
        IDownloadEngine engine = mEngine;
        if (engine != null)
            mEngine.abort();
    }

    public void addItemToQueue(IEpisode argEpisode, @QueuePosition int argPosition) {
        addItemToQueue(argEpisode, true, argPosition);
    }

	/**
	 * Add feeditem to the download queue
	 */
	private void addItemToQueue(IEpisode argEpisode, boolean startedManually, @QueuePosition int argPosition) {
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

        return megabytesToKeep * 1024 * 1024;
    }

    private class DownloadCompleteCallback implements IDownloadEngine.Callback {

        @NonNull private Context mContext;

        DownloadCompleteCallback(@NonNull Context argContext) {
            mContext = argContext;
        }

        @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        @Override
        public void downloadCompleted(IEpisode argEpisode) throws SecurityException {
            FeedItem item = (FeedItem) argEpisode;
            item.setDownloaded(true);

            String mimetype = null;
            try {
                mimetype = StorageUtils.getMimeType(item.getAbsolutePath(mContext));
            } catch (IOException e) {
                e.printStackTrace();
            }

            item.setIsVideo(StorageUtils.getFileType(mimetype) == VIDEO);

            SoundWaves.getAppContext(mContext).getLibraryInstance().updateEpisode(item);

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            try {
                intent.setData(Uri.fromFile(new File(item.getAbsolutePath(mContext))));
            } catch (IOException e) {
                Log.w(TAG, "Could not add file to media scanner"); // NoI18N
                e.printStackTrace();
            }


            mContext.sendBroadcast(intent);

            DownloadService.removeFirst();

            removeDownloadingEpisode(argEpisode);
            StorageUtils.removeExpiredDownloadedPodcasts(mContext);
            StorageUtils.removeTmpFolderCruft(mContext);

            notifyDownloadComplete(argEpisode);
        }

        @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        @Override
        public void downloadInterrupted(IEpisode argEpisode) throws SecurityException {
            removeTopQueueItem();
            removeDownloadingEpisode(argEpisode);
            StorageUtils.removeTmpFolderCruft(mContext);
            notifyDownloadComplete(argEpisode);
        }
    }

    private void notifyDownloadComplete(@Nullable IEpisode argFeedItem) {
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

    public static void postQueueChangedEvent(@Nullable IEpisode argFeedItem, @DownloadManagerEvent int eventType) {
        final DownloadManagerChanged event = produceDownloadManagerState(argFeedItem, eventType);

        Log.d(TAG, "posting DownloadManagerChanged event");
        SoundWaves.getRxBus().send(event);
        Log.d(TAG, "DownloadManagerChanged event posted");
    }

    public static int downloadNewEpisodes(@NonNull Context argContext, @NonNull ISubscription argSubscription) {
        int amountQueued = 0;

        SoundWavesDownloadManager downloadManager = SoundWaves.getAppContext(argContext.getApplicationContext()).getDownloadManager();

        List<IEpisode> episodesToDownload = episodesToDownloadAutomatically(argSubscription);
        for (int i = 0; i < episodesToDownload.size(); i++) {
            IEpisode episode = episodesToDownload.get(i);
            if (downloadNewEpisodeAutomatically(argContext, episode)) {
                amountQueued++;
            }
        }

        return amountQueued;
    }

    static boolean downloadNewEpisodeAutomatically(@NonNull Context argContext, @NonNull IEpisode argEpisode) {
        if (shouldDownloadAutomaticlly(argEpisode)) {
            SoundWavesDownloadManager downloadManager = SoundWaves.getAppContext(argContext.getApplicationContext()).getDownloadManager();
            downloadManager.addItemToQueue(argEpisode, false, ANYWHERE);
            return true;
        }

        return false;
    }

    private static List<IEpisode> episodesToDownloadAutomatically(@NonNull ISubscription argSubscription) {

        List<IEpisode> episodesToDownload = new LinkedList<>();

        SortedList<? extends IEpisode> episodes = argSubscription.getEpisodes();
        FeedItem episode;

        for (int i = 0; i < episodes.size(); i++) {

            episode = (FeedItem)episodes.get(i);

            if (shouldDownloadAutomaticlly(episode)) {
                episodesToDownload.add(episode);
            }
        }

        return episodesToDownload;
    }

    private static boolean shouldDownloadAutomaticlly(@NonNull IEpisode argEpisode) {
        /**
         * Currently we download an episode if:
         *
         * 1) We should download automatically (checked above)
         * 2) It is less than 48 hours old
         * 3) It has not been downloaded before
         *
         */
        Date episodeDate = argEpisode.getDateTime();
        if (episodeDate != null && argEpisode.getCreatedAt().getTime() > 0) {
            episodeDate = argEpisode.getCreatedAt();
        }

        if (episodeDate != null) {
            long createdAt = episodeDate.getTime();
            long now = System.currentTimeMillis();
            long hoursOld = TimeUnit.MILLISECONDS.toHours(now - createdAt);

            if (hoursOld <= HOURS && !argEpisode.hasBeenDownloadedOnce() && argEpisode.isNew()) {
                return true;
            }
        } else {
            Log.w(TAG, "EpisodeDate not set");
        }

        return false;
    }
}
