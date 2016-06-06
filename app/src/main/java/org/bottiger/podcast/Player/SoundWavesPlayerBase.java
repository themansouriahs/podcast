package org.bottiger.podcast.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.player.exoplayer.ExtractorRendererBuilder;
import org.bottiger.podcast.player.soundwaves.NDKMediaPlayer;
import org.bottiger.podcast.R;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;

import java.io.FileDescriptor;
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

    private static final boolean ANDROID_ENGINE_AS_DEFAULT = true;

    private android.media.MediaPlayer mDefaultMediaPlayer;
    private NDKMediaPlayer mCustomMediaPlayer;

    private ExoPlayerWrapper mExoplayer;
    private static final int RENDERER_COUNT = 1;

    public SoundWavesPlayerBase(@NonNull PlayerService argPlayerService) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(argPlayerService);
        String key = argPlayerService.getResources().getString(R.string.pref_use_soundengine_key);
        boolean useCustomEngine = prefs.getBoolean(key, !ANDROID_ENGINE_AS_DEFAULT);

        if (useCustomEngine) {
            mCustomMediaPlayer = new NDKMediaPlayer(argPlayerService, true);
        } else {
            mDefaultMediaPlayer = new android.media.MediaPlayer();
            mDefaultMediaPlayer.reset();
        }

        if (BuildConfig.DEBUG) {
            mExoplayer = new ExoPlayerWrapper();
            mExoplayer.setRenderBuilder(new ExtractorRendererBuilder(argPlayerService, null));
            mType = EXOPLAYER;
            return;
        }

        mType = useCustomEngine ? SOUNDWAVES : ANDROID;
    }

    @Override
    public boolean canSetPitch() {
        switch (mType) {
            case SOUNDWAVES: {
                return mCustomMediaPlayer.canSetPitch();
            }
        }

        return false;
    }

    @Override
    public boolean canSetSpeed() {
        switch (mType) {
            case SOUNDWAVES: {
                return mCustomMediaPlayer.canSetSpeed();
            }
        }

        return false;
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

    @Override
    public long getCurrentPosition() {
        switch (mType) {
            case ANDROID: {
                return mDefaultMediaPlayer.getCurrentPosition();
            }
            case SOUNDWAVES: {
                return mCustomMediaPlayer.getCurrentPosition();
            }
            case EXOPLAYER: {
                return mExoplayer.getCurrentPosition();
            }
        }

        fail();
        return 0;
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        switch (mType) {
            case SOUNDWAVES: {
                return mCustomMediaPlayer.getCurrentSpeedMultiplier();
            }
        }

        return PlaybackSpeed.DEFAULT; // default speed is 1x
    }

    @Override
    public long getDuration() {
        switch (mType) {
            case ANDROID: {
                return mDefaultMediaPlayer.getDuration();
            }
            case SOUNDWAVES: {
                return mCustomMediaPlayer.getDuration();
            }
            case EXOPLAYER: {
                return mExoplayer.getDuration();
            }
        }

        fail();
        return 0;
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
        switch (mType) {
            case ANDROID: {
                return mDefaultMediaPlayer.isLooping();
            }
            case SOUNDWAVES: {
                return mCustomMediaPlayer.isLooping();
            }
            case EXOPLAYER: {
                return false;
            }
        }

        fail();
        return false;
    }

    @Override
    public boolean isPlaying() {
        switch (mType) {
            case ANDROID: {
                try {
                    return mDefaultMediaPlayer.isPlaying();
                } catch (IllegalStateException ise) {
                    return false;
                }
            }
            case SOUNDWAVES: {
                return mCustomMediaPlayer.isPlaying();
            }
            case EXOPLAYER: {
                return mExoplayer.getPlayWhenReady();
            }
        }

        fail();
        return false;
    }

    @Override
    public void pause() {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.pause();
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.pause();
                break;
            }
            case EXOPLAYER: {
                mExoplayer.setPlayWhenReady(false);
                break;
            }
        }
    }

    @Override
    public void prepare() throws IllegalStateException, IOException {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.prepare();
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.prepare();
                break;
            }
            case EXOPLAYER: {
                fail();
                break;
            }
        }
    }

    @Override
    public void prepareAsync() {
        try {
            switch (mType) {
                case ANDROID: {
                    mDefaultMediaPlayer.prepareAsync();
                    break;
                }
                case SOUNDWAVES: {
                    mCustomMediaPlayer.prepareAsync();
                    break;
                }
                case EXOPLAYER: {
                    mExoplayer.prepare();
                    break;
                }
            }
        } catch (Exception ise) {
            Log.e("bad", "very bad");
        }
    }

    @Override
    public void release() {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.release();
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.release();
                break;
            }
            case EXOPLAYER: {
                mExoplayer.release();
                break;
            }
        }
    }

    @Override
    public void reset() {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.reset();
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.reset();
                break;
            }
            case EXOPLAYER: {
                //fail();
                break;
            }
        }
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.seekTo(msec);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.seekTo(msec);
                break;
            }
            case EXOPLAYER: {
                long seekPosition = mExoplayer.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0
                        : Math.min(Math.max(0, msec), getDuration());
                mExoplayer.seekTo(seekPosition);
                break;
            }
        }
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setAudioStreamType(streamtype);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setAudioStreamType(streamtype);
                break;
            }
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setDataSource(context, uri);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setDataSource(context, uri);
                break;
            }
            case EXOPLAYER: {
                ExtractorRendererBuilder audioRenderer = new ExtractorRendererBuilder(context, uri);
                audioRenderer.buildRenderers(mExoplayer);
                //mExoplayer = new ExoPlayerWrapper(audioRenderer);
                mExoplayer.setRenderBuilder(audioRenderer);
                break;
            }
        }
    }

    public void setDataSource(FileDescriptor argfd) throws IllegalArgumentException, IllegalStateException, IOException {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setDataSource(argfd);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setDataSource(argfd.toString()); // FIXME
                break;
            }
        }
    }

    @Override
    public void setDataSource(String path) throws IllegalArgumentException, IllegalStateException, IOException {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setDataSource(path);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setDataSource(path);
                break;
            }
        }
    }

    @Override
    public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) {
        switch (mType) {
            case SOUNDWAVES: {
                mCustomMediaPlayer.setEnableSpeedAdjustment(enableSpeedAdjustment);
            }
        }
    }

    @Override
    public void setLooping(boolean loop) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setLooping(loop);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setLooping(loop);
                break;
            }
        }
    }

    @Override
    public void setPitchStepsAdjustment(float pitchSteps) {
        switch (mType) {
            case SOUNDWAVES: {
                mCustomMediaPlayer.setPitchStepsAdjustment(pitchSteps);
            }
        }
    }

    @Override
    public void setPlaybackPitch(float f) {
        switch (mType) {
            case SOUNDWAVES: {
                mCustomMediaPlayer.setPlaybackPitch(f);
            }
        }
    }

    @Override
    public void setPlaybackSpeed(float f) {
        switch (mType) {
            case SOUNDWAVES: {
                mCustomMediaPlayer.setPlaybackSpeed(f);
            }
        }
    }

    @Override
    public void setSpeedAdjustmentAlgorithm(int algorithm) {
        switch (mType) {
            case SOUNDWAVES: {
                mCustomMediaPlayer.setSpeedAdjustmentAlgorithm(algorithm);
            }
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setVolume(leftVolume, rightVolume);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setVolume(leftVolume, rightVolume);
                break;
            }
        }
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.setWakeMode(context, mode);
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.setWakeMode(context, mode);
                break;
            }
        }
    }

    @Override
    public void start() {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.start();
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.start();
                break;
            }
            case EXOPLAYER: {
                mExoplayer.setPlayWhenReady(true);
                break;
            }
        }
    }

    @Override
    public void stop() {
        switch (mType) {
            case ANDROID: {
                mDefaultMediaPlayer.stop();
                break;
            }
            case SOUNDWAVES: {
                mCustomMediaPlayer.stop();
                break;
            }
        }
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
