package org.bottiger.podcast.service.Downloader.engines;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.R;
import org.bottiger.podcast.notification.ProgressNotification;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.PreferenceHelper;

/**
 * Created by apl on 17-09-2014.
 */
public abstract class DownloadEngineBase implements IDownloadEngine {

    @NonNull private Context mContext;

    private float mProgress = 0;
    private long mDownloadSpeedBps = 0;
    protected IEpisode mEpisode;

    public DownloadEngineBase(@NonNull Context argContext, @NonNull IEpisode argEpisode) {
        mContext = argContext;
        mEpisode = argEpisode;
    }

    public IEpisode getEpisode() {
        return mEpisode;
    }

    @NonNull
    public Context getContext() {
        return mContext;
    }

    protected void setProgress(float argProgress) {

        if (!(mEpisode instanceof FeedItem)) {
            return;
        }

        mProgress = argProgress;
        mEpisode.setProgress(argProgress);
        updatedNotification();
    }

    protected void setSpeed(long argSpeed) {
        mDownloadSpeedBps = argSpeed;
    }

    public float getProgress() {
        return mProgress;
    }

    private void updatedNotification() {
        if (showNotification()) {
            ProgressNotification.show(mContext, getEpisode(), (int) getProgress(), mDownloadSpeedBps);
        }
    }

    protected void removeNotification() {
        if (showNotification()) {
            ProgressNotification.removeNotification(mContext);
        }
    }

    private boolean showNotification() {
        return PreferenceHelper.getBooleanPreferenceValue(mContext,
                R.string.pref_download_notification_key,
                R.bool.pref_download_notification_default);
    }
}
