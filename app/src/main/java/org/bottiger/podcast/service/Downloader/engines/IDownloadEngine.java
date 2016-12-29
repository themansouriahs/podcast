package org.bottiger.podcast.service.Downloader.engines;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 17-09-2014.
 */
public interface IDownloadEngine {

    void startDownload(boolean argIsLast);
    float getProgress();
    IEpisode getEpisode();
    void addCallback(Callback argCallback);
    void abort();

    interface Callback {
        void downloadCompleted(IEpisode argEpisode);
        void downloadInterrupted(IEpisode argEpisode);
    }
}
