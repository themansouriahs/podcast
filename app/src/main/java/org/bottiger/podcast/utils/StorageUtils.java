package org.bottiger.podcast.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by aplb on 07-04-2016.
 */
public class StorageUtils {

    private static final String TAG = StorageUtils.class.getSimpleName();

    private static final String MIME_AUDIO = "audio";
    private static final String MIME_VIDEO = "video";
    private static final String MIME_OTHER = "other";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AUDIO, VIDEO, OTHER})
    public @interface MimeType {}
    public static final int AUDIO = 1;
    public static final int VIDEO = 2;
    public static final int OTHER = 3;

    /**
     * Removes all the expired downloads async
     */
    public static void removeExpiredDownloadedPodcasts(Context context) {
        removeExpiredDownloadedPodcastsTask(context);
    }


    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static boolean removeTmpFolderCruft(@NonNull Context argContext) throws SecurityException {
        String tmpFolder;
        try {
            tmpFolder = SDCardManager.getTmpDir(argContext);
        } catch (IOException e) {
            Log.w(TAG, "Could not access tmp storage. removeTmpFolderCruft() returns without success"); // NoI18N
            return false;
        }
        Log.d(TAG, "Cleaning tmp folder: " + tmpFolder); // NoI18N
        File dir = new File(tmpFolder);
        if(dir.exists() && dir.isDirectory()) {
            return FileUtils.cleanDirectory(dir, false);
        }

        return  true;
    }

    /**
     * Iterates through all the downloaded episodes and deletes the ones who
     * exceed the download limit Runs with minimum priority
     */
    @WorkerThread
    private static void removeExpiredDownloadedPodcastsTask(Context context) throws SecurityException {

        if (BuildConfig.DEBUG && Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Should not be executed on main thread!");
        }

        if (!SDCardManager.getSDCardStatus()) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long bytesToKeep = SoundWavesDownloadManager.bytesToKeep(sharedPreferences, context.getResources());
        ArrayList<IEpisode> episodes = SoundWaves.getAppContext(context).getLibraryInstance().getEpisodes();
        LinkedList<String> filesToKeep = new LinkedList<>();

        IEpisode episode;
        FeedItem item;
        File file;
        boolean isFeedItem;
        long lastModifiedKey;

        // Build list of downloaded files
        SortedMap<Long, FeedItem> sortedMap = new TreeMap<>();
        for (int i = 0; i < episodes.size(); i++) {

            // Extract data.
            episode = episodes.get(i);
            isFeedItem = episode instanceof FeedItem;

            if (!isFeedItem) {
                continue;
            }

            item = (FeedItem) episode;

            if (item.isDownloaded(context)) {
                try {
                    file = new File(item.getAbsolutePath(context));
                    lastModifiedKey = file.lastModified();
                    sortedMap.put(-lastModifiedKey, item);
                } catch (IOException e) {
                    ErrorUtils.handleException(e);
                }
            }
        }

        SortedSet<Long> keys = new TreeSet<>(sortedMap.keySet());
        for (Long key : keys) {
            boolean deleteFile = true;

            item = sortedMap.get(key);
            try {
                file = new File(item.getAbsolutePath(context));
            } catch (IOException e) {
                ErrorUtils.handleException(e);
                continue;
            }

            if (file.exists()) {
                bytesToKeep = bytesToKeep - item.getFilesize();

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
        File directory;
        try {
            directory = SDCardManager.getDownloadDir(context);
        } catch (IOException e) {
            ErrorUtils.handleException(e);
            return;
        }

        File[] files = directory.listFiles();
        for (File keepFile : files) {
            if (!filesToKeep.contains(keepFile.getName())) {
                // Delete each file
                keepFile.delete();
            }
        }
    }

    public static boolean canPerform(@SoundWavesDownloadManager.Action int argAction,
                                     @NonNull Context argContext,
                                     @NonNull ISubscription argSubscription) {
        Log.v(TAG, "canPerform: " + argAction);

        boolean isLargeFile = argAction == SoundWavesDownloadManager.ACTION_DOWNLOAD_AUTOMATICALLY;
        @SoundWavesDownloadManager.NetworkState int networkState = NetworkUtils.getNetworkStatus(argContext, isLargeFile);

        if (networkState == SoundWavesDownloadManager.NETWORK_DISCONNECTED)
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
            case SoundWavesDownloadManager.ACTION_DOWNLOAD_AUTOMATICALLY: {
                if (!automaticDownload)
                    return false;

                if (wifiOnly)
                    return networkState == SoundWavesDownloadManager.NETWORK_OK;
                else
                    return networkState == SoundWavesDownloadManager.NETWORK_OK || networkState == SoundWavesDownloadManager.NETWORK_RESTRICTED;
            }
            case SoundWavesDownloadManager.ACTION_DOWNLOAD_MANUALLY:
            case SoundWavesDownloadManager.ACTION_REFRESH_SUBSCRIPTION:
            case SoundWavesDownloadManager.ACTION_STREAM_EPISODE: {
                return networkState == SoundWavesDownloadManager.NETWORK_OK || networkState == SoundWavesDownloadManager.NETWORK_RESTRICTED;
            }
        }

        VendorCrashReporter.report(TAG, "canPerform defaults to false. Action: " + argAction);
        return false; // FIXME this should never happen. Ensure we never get here
    }

    /**
     * Deletes the downloaded file and updates the database record
     *
     * @param context
     */
    private static void deleteExpireFile(@NonNull Context context, FeedItem item) throws SecurityException {
        if (item == null)
            return;

        item.delFile(context);
    }

    public static @MimeType int getFileType(@Nullable String argMimeType) {
        if (TextUtils.isEmpty(argMimeType))
            return OTHER;

        String lowerCase = argMimeType.toLowerCase();

        if (lowerCase.contains(MIME_AUDIO))
            return AUDIO;

        if (lowerCase.contains(MIME_VIDEO))
            return VIDEO;

        return OTHER;
    }

    public static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public static int getStorageCapacityGB(@NonNull Context argContext) {
        String collectionSize = PreferenceHelper.getStringPreferenceValue(argContext,
                R.string.pref_podcast_collection_size_key,
                R.string.pref_download_collection_size_default);

        return (int) (Long.valueOf(collectionSize) / 1000);
    }

    public static void openFolderIntent(@NonNull Context argContext, @NonNull String argLocation)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(argLocation);
        //intent.setDataAndType(uri, "text/csv");
        intent.setDataAndType(uri, "text/csv");
        argContext.startActivity(Intent.createChooser(intent, "Open folder"));
    }
}
