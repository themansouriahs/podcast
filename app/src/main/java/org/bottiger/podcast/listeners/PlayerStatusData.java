package org.bottiger.podcast.listeners;

/**
 * Created by apl on 28-05-2015.
 */
public class PlayerStatusData {

    public PlayerStatusObservable.STATUS status = PlayerStatusObservable.STATUS.STOPPED;

    public PlayerStatusData(PlayerStatusObservable.STATUS argStatus) {
        status = argStatus;
    }
    /*
        public IEpisode getEpisode();
    public void setProgressMs(long progressMs); // progress in ms
    public void onStateChange(EpisodeStatus argStatus);
     */

}
