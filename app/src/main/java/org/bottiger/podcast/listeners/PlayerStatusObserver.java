package org.bottiger.podcast.listeners;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 04-08-2014.
 */
public interface PlayerStatusObserver {

    public IEpisode getEpisode();
    public void setProgressMs(long progressMs); // progress in ms
    public void onStateChange(EpisodeStatus argStatus);
}
