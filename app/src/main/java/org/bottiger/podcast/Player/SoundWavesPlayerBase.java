package org.bottiger.podcast.player;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.listeners.PlayerStatusData;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.player.exoplayer.ExtractorRendererBuilder;
import org.bottiger.podcast.player.exoplayer.PodcastAudioRendererV21;
import org.bottiger.podcast.R;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arvid on 8/27/2015.
 */
public abstract class SoundWavesPlayerBase implements GenericMediaPlayerInterface {

    protected  @PlayerStatusObservable.PlayerStatus int mStatus;

    @Nullable
    protected PlayerService mPlayerService;
    protected org.bottiger.podcast.player.PlayerHandler mHandler;
    protected org.bottiger.podcast.player.PlayerStateManager mPlayerStateManager;

    // AudioManager
    protected AudioManager mAudioManager;
    protected ComponentName mControllerComponentName;

    private boolean mIsInitialized = false;

    public SoundWavesPlayerBase(@NonNull Context argContext) {

    }

    public void setPlayerService(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
        mPlayerStateManager = argPlayerService.getPlayerStateManager();
        this.mControllerComponentName = new ComponentName(mPlayerService,
                HeadsetReceiver.class);
        this.mAudioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setDataSourceAsync(String path, int startPos) {
        mStatus = PlayerStatusObservable.PREPARING;
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_CONNECTING, startPos, getCurrentSpeedMultiplier());
    }

    public void start() {
        mStatus = PlayerStatusObservable.PLAYING;
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), getCurrentSpeedMultiplier());

        PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.PLAYING);
        SoundWaves.getBus().post(psd);
    }

    public void stop() {
        mStatus = PlayerStatusObservable.STOPPED;
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_STOPPED, getCurrentPosition(), getCurrentSpeedMultiplier());
        mIsInitialized = false;
        mPlayerService.stopForeground(true);
        PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.STOPPED);
        SoundWaves.getBus().post(psd);
    }

    public @PlayerStatusObservable.PlayerStatus int getStatus() {
        return mStatus;
    }

    public void toggle() {
        if (isPlaying()) {
            pause();
        } else {
            start();
        }
    }

    public void release() {

        // Called when the PlayerService is destroyed.
        // Therefore mPlayerService will be null.
        if (mPlayerService != null) {
            mPlayerService.dis_notifyStatus();
        }

        stop();
        mIsInitialized = false;
        mStatus = PlayerStatusObservable.STOPPED;
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isCasting() {
        return false;
    }

    public void addListener(ExoPlayerWrapper.Listener listener) {
    }

    public void removeListener(ExoPlayerWrapper.Listener listener) {
    }

    public void setHandler(PlayerHandler handler) {
        mHandler = handler;
    }

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
    }

    private void fail() throws AssertionError {
        throw new AssertionError("This should never happen");
    }

    public boolean doAutomaticGainControl() {
        return false;
    }

    public void setAutomaticGainControl(boolean argSetAutomaticGainControl) {
    }

    public boolean doRemoveSilence() {
        return false;
    }

    public void setRemoveSilence(boolean argDoRemoveSilence) {
    }

}
