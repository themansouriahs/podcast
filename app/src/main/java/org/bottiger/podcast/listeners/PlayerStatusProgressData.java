package org.bottiger.podcast.listeners;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 28-05-2015.
 */
public class PlayerStatusProgressData {

    public IEpisode mEpisode;

    public PlayerStatusProgressData(@NonNull IEpisode argEpisode) {
        mEpisode = argEpisode;
    }

    @NonNull
    public IEpisode getEpisode() {
        return mEpisode;
    }

    public long getProgress() {
        return Math.max(mEpisode.getOffset(), 0);
    }
}
