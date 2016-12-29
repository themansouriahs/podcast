package org.bottiger.podcast.service.Downloader.engines;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.FeedItem;

/**
 * Created by apl on 17-09-2014.
 */
public class AndroidDownloader extends DownloadEngineBase {

    public AndroidDownloader(@NonNull Context argContext, @NonNull FeedItem argEpisode) {
        super(argContext, argEpisode);
    }

    @Override
    public void startDownload(boolean argIsLast) {

    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public void addCallback(Callback argCallback) {

    }

    @Override
    public void abort() {

    }
}
