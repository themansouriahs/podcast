package org.bottiger.podcast.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;

import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import java.io.IOException;

/**
 * Created by Arvid on 8/28/2015.
 */
public interface GenericMediaPlayerInterface {

    void setDataSourceAsync(@NonNull IEpisode argEpisode);
    @PlayerStatusObservable.PlayerStatus int getStatus();
    boolean isSteaming();
    boolean isInitialized();

    void toggle();
    void start();
    void stop();
    void release();
    boolean isPlaying();

    long getCurrentPosition();

    void rewind(@Nullable IEpisode argItem);
    void fastForward(@Nullable IEpisode argItem);

    void updateNotificationPlayer();
    void removeNotificationPlayer();

    void pause();

    float getVolume();
    void setVolume(float vol);

    boolean isCasting();
    void setPlayerService(@NonNull PlayerService argPlayerService);

    void addListener(ExoPlayer.EventListener listener);
    void removeListener(ExoPlayer.EventListener listener);

    void startAndFadeIn();
    void FaceOutAndStop(int argDelayMs);


    boolean doAutomaticGainControl();
    void setAutomaticGainControl(boolean argSetAutomaticGainControl);
    boolean doRemoveSilence();
    void setRemoveSilence(boolean argDoRemoveSilence);

    boolean canSetPitch();
    boolean canSetSpeed();
    float getCurrentPitchStepsAdjustment();
    float getCurrentSpeedMultiplier();
    long getDuration();
    float getMaxSpeedMultiplier();
    float getMinSpeedMultiplier();
    void prepare() throws IllegalStateException, IOException;
    void reset();
    long seekTo(long msec) throws IllegalStateException;
    void setAudioStreamType(int streamtype);
    void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException;
    void setPlaybackSpeed(float f);

    // For registering listeners
    interface OnBufferingUpdateListener {
        void onBufferingUpdate(GenericMediaPlayerInterface arg0, int percent);
    }

    interface OnCompletionListener {
        void onCompletion(GenericMediaPlayerInterface arg0);
    }

    interface OnErrorListener {
        boolean onError(GenericMediaPlayerInterface arg0, int what, int extra);
    }

    void setOnErrorListener(OnErrorListener listener);
    void setOnCompletionListener(OnCompletionListener listener);
    void setOnBufferingUpdateListener(OnBufferingUpdateListener listener);
}
