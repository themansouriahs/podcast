package org.bottiger.podcast.service.Downloader.engines;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 17-09-2014.
 */
public abstract class DownloadEngineBase implements IDownloadEngine {

    protected IEpisode mEpisode;

    public DownloadEngineBase(@NonNull IEpisode argEpisode) {
        mEpisode = argEpisode;
    }

    public IEpisode getEpisode() {
        return mEpisode;
    }
}
