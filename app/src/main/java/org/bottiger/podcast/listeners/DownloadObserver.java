package org.bottiger.podcast.listeners;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 24-08-2014.
 */
public interface DownloadObserver {

    public IEpisode getEpisode();
    public void setProgressPercent(int argProgress);
}
