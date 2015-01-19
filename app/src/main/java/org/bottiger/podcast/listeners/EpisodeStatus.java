package org.bottiger.podcast.listeners;

/**
 * Created by apl on 06-09-2014.
 */
public class EpisodeStatus {

    private long mEpisodeId = -1;
    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;
    private int mPosition = -1;

    public EpisodeStatus(long argEpisodeId, PlayerStatusObservable.STATUS argStatus) {
        mEpisodeId = argEpisodeId;
        mStatus = argStatus;
    }

    public EpisodeStatus(long argEpisodeId, PlayerStatusObservable.STATUS argStatus, int argPosition) {
        mEpisodeId = argEpisodeId;
        mStatus = argStatus;
        mPosition = argPosition;
    }

    public static EpisodeStatus generateEpisodeStatsus(long argEpisodeId, PlayerStatusObservable.STATUS argStatus, Integer argPosition) {
        if (argPosition == null) {
            return new EpisodeStatus(argEpisodeId, argStatus);
        }
        return new EpisodeStatus(argEpisodeId, argStatus, argPosition);
    }

    public long getEpisodeId() {
        return mEpisodeId;
    }

    public int getPlaybackPositionMs() {
        return mPosition;
    }

    public PlayerStatusObservable.STATUS getStatus() {
        return mStatus;
    }
}
