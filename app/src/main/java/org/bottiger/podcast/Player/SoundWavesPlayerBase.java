package org.bottiger.podcast.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.google.android.exoplayer.TrackRenderer;

import org.bottiger.podcast.R;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.player.exoplayer.ExtractorRendererBuilder;
import org.bottiger.podcast.player.soundwaves.NDKMediaPlayer;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arvid on 8/27/2015.
 */
public abstract class SoundWavesPlayerBase implements GenericMediaPlayerInterface {

    @IntDef({ANDROID, SOUNDWAVES, EXOPLAYER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerType {}

    public static final int ANDROID = 0;
    public static final int SOUNDWAVES = 1;
    public static final int EXOPLAYER = 2;

    @PlayerType int mType;

    private android.media.MediaPlayer mDefaultMediaPlayer;
    private NDKMediaPlayer mCustomMediaPlayer;

    private ExoPlayerWrapper mExoplayer;
    private static final int RENDERER_COUNT = 1;

    private TrackRenderer[] mTrackRendere = new TrackRenderer[ExoPlayerWrapper.RENDERER_COUNT];

    public SoundWavesPlayerBase(@NonNull Context argContext) {
        boolean remove_silence = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_audioengine_remove_silence_key,
                R.bool.pref_audioengine_remove_silence_default);

        boolean gain_control = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_audioengine_automatic_gain_control_key,
                R.bool.pref_audioengine_automatic_gain_control_default);

        mExoplayer = new ExoPlayerWrapper();
        mExoplayer.setRemoveSilence(remove_silence);
        mExoplayer.setAutomaticGainControl(gain_control);
        mExoplayer.setRenderBuilder(new ExtractorRendererBuilder(argContext, null));
        mType = EXOPLAYER;
    }

    public void addListener(ExoPlayerWrapper.Listener listener) {
        mExoplayer.addListener(listener);
    }

    public void removeListener(ExoPlayerWrapper.Listener listener) {
        mExoplayer.removeListener(listener);
    }

    @Override
    public boolean canSetPitch() {
        return true;
    }

    @Override
    public boolean canSetSpeed() {
        return true;
    }

    @Override
    public float getCurrentPitchStepsAdjustment() {
        switch (mType) {
            case SOUNDWAVES: {
                return mCustomMediaPlayer.getCurrentPitchStepsAdjustment();
            }
        }

        return 0;
    }

    public boolean doAutomaticGainControl() {
        return mExoplayer.doAutomaticGainControl();
    }

    public void setAutomaticGainControl(boolean argSetAutomaticGainControl) {
        mExoplayer.setAutomaticGainControl(argSetAutomaticGainControl);
    }

    public boolean doRemoveSilence() {
        return mExoplayer.doRemoveSilence();
    }

    public void setRemoveSilence(boolean argDoRemoveSilence) {
        mExoplayer.setRemoveSilence(argDoRemoveSilence);
    }

    @Override
    public long getCurrentPosition() {
        return mExoplayer.getCurrentPosition();
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        return PlaybackSpeed.DEFAULT; // default speed is 1x
    }

    @Override
    public long getDuration() {
        return mExoplayer.getDuration();
    }

    @Override
    public float getMaxSpeedMultiplier() {
        switch (mType) {
            case SOUNDWAVES: {
                return mCustomMediaPlayer.getMaxSpeedMultiplier();
            }
        }

        return 0;
    }

    @Override
    public float getMinSpeedMultiplier() {
        switch (mType) {
            case SOUNDWAVES: {
                return mCustomMediaPlayer.getMinSpeedMultiplier();
            }
        }

        return 0;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return mExoplayer.getPlayWhenReady();
    }

    @Override
    public void pause() {
        mExoplayer.setPlayWhenReady(false);
    }

    @Override
    @Deprecated
    public void prepareAsync() {
        mExoplayer.prepare();
    }

    @Override
    public void prepare() {
        mExoplayer.prepare();
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException {
        ExtractorRendererBuilder audioRenderer = new ExtractorRendererBuilder(context, uri);
        mExoplayer.setRenderBuilder(audioRenderer);
    }

    @Override
    public void release() {
        mExoplayer.release();
    }

    @Override
    public void reset() {
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        mExoplayer.seekTo(msec);
    }

    @Override
    public void setAudioStreamType(int streamtype) {
    }

    @Override
    public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) {
    }

    @Override
    public void setLooping(boolean loop) {
    }

    @Override
    public void setPitchStepsAdjustment(float pitchSteps) {
    }

    @Override
    public void setPlaybackPitch(float f) {
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void setPlaybackSpeed(float speed) {
        mExoplayer.setPlaybackSpeed(speed);
    }

    @Override
    public void setSpeedAdjustmentAlgorithm(int algorithm) {
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    @Override
    public void setWakeMode(Context context, int mode) {
    }

    @Override
    public void start() {
        mExoplayer.setPlayWhenReady(true);
    }

    @Override
    public void stop() {
        mExoplayer.release();
    }

    @Override
    public void setOnPreparedListener(final OnPreparedListener listener) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(android.media.MediaPlayer mp) {
                        listener.onPrepared(SoundWavesPlayerBase.this);
                    }
                });
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setOnPreparedListener(new NDKMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(NDKMediaPlayer arg0) {
                        listener.onPrepared(SoundWavesPlayerBase.this);
                    }
                });
                break;
            }
        }
    }

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                        return listener.onError(SoundWavesPlayerBase.this, what, extra);
                    }
                });
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setOnErrorListener(new NDKMediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(NDKMediaPlayer arg0, int what, int extra) {
                        return listener.onError(SoundWavesPlayerBase.this, what, extra);
                    }
                });
                break;
            }
        }
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(android.media.MediaPlayer mp) {
                        listener.onCompletion(SoundWavesPlayerBase.this);
                    }
                });
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setOnCompletionListener(new NDKMediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(NDKMediaPlayer arg0) {
                        listener.onCompletion(SoundWavesPlayerBase.this);
                    }
                });
                break;
            }
        }
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setOnBufferingUpdateListener(new android.media.MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
                        listener.onBufferingUpdate(SoundWavesPlayerBase.this, percent);
                    }
                });
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setOnBufferingUpdateListener(new NDKMediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(NDKMediaPlayer arg0, int percent) {
                        listener.onBufferingUpdate(SoundWavesPlayerBase.this, percent);
                    }
                });
                break;
            }
        }
    }

    private void fail() throws AssertionError {
        throw new AssertionError("This should never happen");
    }
}
