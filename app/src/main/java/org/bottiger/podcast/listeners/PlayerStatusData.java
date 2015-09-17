package org.bottiger.podcast.listeners;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 28-05-2015.
 */
public class PlayerStatusData {

    public @PlayerStatusObservable.PlayerStatus int status = PlayerStatusObservable.STOPPED;
    public IEpisode episode = null;

    public PlayerStatusData(IEpisode argEpisode, @PlayerStatusObservable.PlayerStatus int argStatus) {
        status = argStatus;
        episode = argEpisode;
    }
}
