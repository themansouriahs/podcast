package org.bottiger.podcast.utils;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.service.Downloader.engines.OkHttpDownloader;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by aplb on 07-04-2016.
 */
public class NetworkUtils {

    private static final String TAG = StorageUtils.class.getSimpleName();

    public static boolean isWiFiOnly(@NonNull Context argContext, boolean argIsLargeFile) {
        if (argIsLargeFile) {
            return PreferenceHelper.getBooleanPreferenceValue(argContext,
                    R.string.pref_download_only_wifi_key,
                    R.bool.pref_download_only_wifi_default);
        }

        return PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_refresh_only_wifi_key,
                R.bool.pref_refresh_only_wifi_default);
    }

    public static @SoundWavesDownloadManager.NetworkState
    int getNetworkStatus(@NonNull Context argContext, boolean argIsLargeFile) {
		Log.v(TAG, "getNetworkStatus");

        ConnectivityManager cm = (ConnectivityManager) argContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return SoundWavesDownloadManager.NETWORK_DISCONNECTED;
        }

        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info == null) {
            return SoundWavesDownloadManager.NETWORK_DISCONNECTED;
        }

        if (!info.isConnected()) {
            return SoundWavesDownloadManager.NETWORK_DISCONNECTED;
        }

        int networkType = info.getType();

        switch (networkType) {
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_VPN:
                return SoundWavesDownloadManager.NETWORK_OK;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            {
                return isWiFiOnly(argContext, argIsLargeFile) ? SoundWavesDownloadManager.NETWORK_RESTRICTED : SoundWavesDownloadManager.NETWORK_OK;
            }
        }

        return SoundWavesDownloadManager.NETWORK_OK;
	}

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static boolean canDownload(QueueEpisode nextInQueue,
                                      @NonNull Context argContext,
                                      @NonNull ReentrantLock argLock) throws SecurityException {
        // Make sure we have access to external storage
        if (!SDCardManager.getSDCardStatusAndCreate()) {
            return false;
        }

        @SoundWavesDownloadManager.NetworkState int networkState = getNetworkStatus(argContext, true);

        FeedItem downloadingItem;
        try {
            argLock.lock();

            downloadingItem = SoundWaves.getAppContext(argContext).getLibraryInstance().getEpisode(nextInQueue.getId());

            if (downloadingItem == null)
                return false;

            if (!StrUtils.isValidUrl(downloadingItem.getURL()))
                return false;

            if (!nextInQueue.IsStartedManually() && networkState != SoundWavesDownloadManager.NETWORK_OK) {
                return false;
            }

            if (!nextInQueue.IsStartedManually() && !(networkState == SoundWavesDownloadManager.NETWORK_OK || networkState == SoundWavesDownloadManager.NETWORK_RESTRICTED)) {
                return false;
            }
        } finally {
            argLock.unlock();
        }

        return true;
    }

    @WorkerThread
    public static IDownloadEngine newEngine(@NonNull Context argContext, @NonNull FeedItem argEpisode) {
        return new OkHttpDownloader(argContext, argEpisode);
    }
}
