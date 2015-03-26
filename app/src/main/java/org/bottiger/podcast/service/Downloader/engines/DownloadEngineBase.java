package org.bottiger.podcast.service.Downloader.engines;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.FeedItem;

/**
 * Created by apl on 17-09-2014.
 */
public abstract class DownloadEngineBase implements IDownloadEngine {

    protected FeedItem mEpisode;

    public DownloadEngineBase(@NonNull FeedItem argEpisode) {
        mEpisode = argEpisode;
    }

    public FeedItem getEpisode() {
        return mEpisode;
    }
}
