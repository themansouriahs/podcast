package org.bottiger.podcast.utils;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.bottiger.podcast.provider.IEpisode;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by aplb on 05-04-2016.
 */
public class PlayerHelper {

    private MediaControllerCompat mMediaControllerCompat;
    private ReentrantLock mLock = new ReentrantLock();

    public void setMediaControllerCompat(@NonNull MediaControllerCompat argMediaControllerCompat) {
        try {
            mLock.lock();
            mMediaControllerCompat = argMediaControllerCompat;
        } finally {
            mLock.unlock();
        }
    }

    /**
     *
     * @return true if the player is playing
     */
    public boolean isPlaying() {
        try {
            mLock.lock();
            return mMediaControllerCompat != null &&
                mMediaControllerCompat.getPlaybackState() != null &&
                mMediaControllerCompat.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
        } finally {
            mLock.unlock();
        }
    }

    /**
     *
     * @return true if the player is playing the episode
     */
    public boolean isPlaying(@NonNull IEpisode argEpisode) {
        try {
            mLock.lock();
            if (!isPlaying()) {
                return false;
            }

            MediaMetadataCompat metadataCompat = mMediaControllerCompat.getMetadata();
            if (metadataCompat.containsKey(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
                String id = metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

                return argEpisode.getURL().equals(id);
            }

            return false;
        } finally {
            mLock.unlock();
        }
    }
}
