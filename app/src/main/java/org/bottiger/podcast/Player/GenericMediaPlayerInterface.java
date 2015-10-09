package org.bottiger.podcast.player;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

/**
 * Created by Arvid on 8/28/2015.
 */
public interface GenericMediaPlayerInterface {

    boolean canSetPitch();
    boolean canSetSpeed();
    float getCurrentPitchStepsAdjustment();
    int getCurrentPosition();
    float getCurrentSpeedMultiplier();
    int getDuration();
    float getMaxSpeedMultiplier();
    float getMinSpeedMultiplier();
    boolean isLooping();
    boolean isPlaying();
    void pause();
    void prepare() throws IllegalStateException, IOException;
    void prepareAsync();
    void release();
    void reset();
    void seekTo(int msec) throws IllegalStateException;
    void setAudioStreamType(int streamtype);
    void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException;
    void setDataSource(String path) throws IllegalArgumentException, IllegalStateException, IOException;
    void setEnableSpeedAdjustment(boolean enableSpeedAdjustment);
    void setLooping(boolean loop);
    void setPitchStepsAdjustment(float pitchSteps);
    void setPlaybackPitch(float f);
    void setPlaybackSpeed(float f);
    void setSpeedAdjustmentAlgorithm(int algorithm);
    void setVolume(float leftVolume, float rightVolume);
    void setWakeMode(Context context, int mode);
    void start();
    void stop();


    // For registering listeners
    interface OnBufferingUpdateListener {
        void onBufferingUpdate(GenericMediaPlayerInterface arg0, int percent);
    }

    interface OnCompletionListener {
        void onCompletion(GenericMediaPlayerInterface arg0);
    }

    interface OnErrorListener {
        public abstract boolean onError(GenericMediaPlayerInterface arg0, int what, int extra);
    }

    interface OnInfoListener {
        public abstract boolean onInfo(GenericMediaPlayerInterface arg0, int what, int extra);
    }

    interface OnPitchAdjustmentAvailableChangedListener {
        /**
         * @param arg0                     The owning media player
         * @param pitchAdjustmentAvailable True if pitch adjustment is available, false if not
         */
        public abstract void onPitchAdjustmentAvailableChanged(
                GenericMediaPlayerInterface arg0, boolean pitchAdjustmentAvailable);
    }

    interface OnPreparedListener {
        public abstract void onPrepared(GenericMediaPlayerInterface arg0);
    }

    interface OnSeekCompleteListener {
        public abstract void onSeekComplete(GenericMediaPlayerInterface arg0);
    }

    interface OnSpeedAdjustmentAvailableChangedListener {
        /**
         * @param arg0                     The owning media player
         * @param speedAdjustmentAvailable True if speed adjustment is available, false if not
         */
        public abstract void onSpeedAdjustmentAvailableChanged(
                GenericMediaPlayerInterface arg0, boolean speedAdjustmentAvailable);
    }

    void setOnPreparedListener(OnPreparedListener listener);
    void setOnErrorListener(OnErrorListener listener);
    void setOnCompletionListener(OnCompletionListener listener);
    void setOnBufferingUpdateListener(OnBufferingUpdateListener listener);
}
