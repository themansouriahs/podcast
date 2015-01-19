package org.bottiger.podcast.listeners;

import org.bottiger.podcast.provider.FeedItem;

/**
 * Created by apl on 24-08-2014.
 */
public interface DownloadObserver {

    public FeedItem getEpisode();
    public void setProgressPercent(int argProgress);
}
