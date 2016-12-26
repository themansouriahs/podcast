package org.bottiger.podcast.utils;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.TextView;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.IEpisode;

import java.util.HashMap;
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

            if (metadataCompat == null) {
                return false;
            }

            if (metadataCompat.containsKey(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
                String id = metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

                String url = argEpisode.getURL();

                if (url == null)
                    return false;

                return argEpisode.getURL().equals(id);
            }

            return false;
        } finally {
            mLock.unlock();
        }
    }

    public static long setDuration(@NonNull IEpisode argEpisode, @NonNull TextView argTextView) {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        Uri location = argEpisode.getFileLocation(IEpisode.PREFER_LOCAL, argTextView.getContext());

        if (location == null) {
            argTextView.setText("");
            VendorCrashReporter.report("setDuration Failed", "Could not aquire location of: episode:" + argEpisode);
            return -1;
        }

        try {
            metaRetriever.setDataSource(location.toString(), new HashMap<String, String>());
        } catch (Exception e) {
            argTextView.setText("");
            VendorCrashReporter.report("Invalid location", "location:" + location.getPath());
            return -1;
        }

        String durationStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long durationMs = Long.parseLong(durationStr);

        String formattedDurationStr = StrUtils.formatTime(durationMs);

        argTextView.setText(formattedDurationStr);

        argEpisode.setDuration(durationMs);

        metaRetriever.release();

        return durationMs;
    }
}
