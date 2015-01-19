package org.bottiger.podcast.service.Downloader;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.FeedItem;

import java.net.URL;

/**
 * Created by apl on 17-09-2014.
 */
public class AndroidDownloader extends DownloadEngineBase {

    public AndroidDownloader(@NonNull FeedItem argEpisode) {
        super(argEpisode);
    }

    @Override
    public void startDownload() {

    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public void addCallback(Callback argCallback) {

    }
}
