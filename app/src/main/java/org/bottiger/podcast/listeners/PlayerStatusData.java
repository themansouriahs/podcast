package org.bottiger.podcast.listeners;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 28-05-2015.
 */
public class PlayerStatusData {

    public PlayerStatusObservable.STATUS status = PlayerStatusObservable.STATUS.STOPPED;
    public IEpisode episode = null;

    public PlayerStatusData(IEpisode argEpisode, PlayerStatusObservable.STATUS argStatus) {
        status = argStatus;
        episode = argEpisode;
    }
    /*
        public IEpisode getEpisode();
    public void setProgressMs(long progressMs); // progress in ms
    public void onStateChange(EpisodeStatus argStatus);
     */

}
