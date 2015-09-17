package org.bottiger.podcast.listeners;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 06-09-2014.
 */
public class EpisodeStatus {

    private IEpisode mEpisode = null;
    private @PlayerStatusObservable.PlayerStatus int mStatus = PlayerStatusObservable.STOPPED;
    private int mPosition = -1;

    public EpisodeStatus(IEpisode argEpisode, @PlayerStatusObservable.PlayerStatus int argStatus) {
        mEpisode = argEpisode;
        mStatus = argStatus;
    }

    public EpisodeStatus(IEpisode argEpisode, @PlayerStatusObservable.PlayerStatus int argStatus, int argPosition) {
        mEpisode = argEpisode;
        mStatus = argStatus;
        mPosition = argPosition;
    }

    public static EpisodeStatus generateEpisodeStatsus(IEpisode argEpisode, @PlayerStatusObservable.PlayerStatus int argStatus, Integer argPosition) {
        if (argPosition == null) {
            return new EpisodeStatus(argEpisode, argStatus);
        }
        return new EpisodeStatus(argEpisode, argStatus, argPosition);
    }

    public IEpisode getEpisode() {
        return mEpisode;
    }

    public int getPlaybackPositionMs() {
        return mPosition;
    }

    public @PlayerStatusObservable.PlayerStatus int getStatus() {
        return mStatus;
    }
}
