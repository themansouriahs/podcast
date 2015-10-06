package org.bottiger.podcast.service.Downloader.engines;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

import java.io.File;
import java.net.URL;

/**
 * Created by apl on 17-09-2014.
 */
public interface IDownloadEngine {

    void startDownload();
    float getProgress();
    IEpisode getEpisode();
    void addCallback(Callback argCallback);
    void abort();

    interface Callback {
        void downloadCompleted(IEpisode argEpisode);
        void downloadInterrupted(IEpisode argEpisode);
    }
}
