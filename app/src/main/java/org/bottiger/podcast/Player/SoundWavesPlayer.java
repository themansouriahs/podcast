package org.bottiger.podcast.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.exoplayer.ExoPlayerMediaSourceHelper;
import org.bottiger.podcast.player.exoplayer.NewExoPlayer;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.io.File;
import java.io.IOException;

import static org.bottiger.podcast.service.PlayerService.getCurrentItem;

/**
 * Created by apl on 20-01-2015.
 */
public class SoundWavesPlayer extends org.bottiger.podcast.player.SoundWavesPlayerBase {

    private static final String TAG = "SoundWavesPlayerBase";

    private static final float MARK_AS_LISTENED_RATIO_THRESHOLD = 0.9f;
    private static final float MARK_AS_LISTENED_MINUTES_LEFT_THRESHOLD = 5f;

    private boolean mIsStreaming = false;

    private boolean isPreparingMedia = false;

    int bufferProgress = 0;
    float playbackSpeed = 1.0f;

    @NonNull
    private NewExoPlayer mExoplayer;

    private PlayerHandler mPlayerHandler;

    public SoundWavesPlayer(@NonNull final Context argContext) {
        super(argContext);

        boolean remove_silence = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_audioengine_remove_silence_key,
                R.bool.pref_audioengine_remove_silence_default);

        boolean gain_control = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_audioengine_automatic_gain_control_key,
                R.bool.pref_audioengine_automatic_gain_control_default);


        mExoplayer = NewExoPlayer.newInstance(argContext);
        mPlayerHandler = new PlayerHandler(argContext);

        addListener(new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
                Log.d(TAG, "loading chanced: " + isLoading);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, @PlayerState int playbackState) {
                Log.d(TAG, "player state changed: " + playbackState + " playWhenReady: " + playWhenReady);

                int state = PlaybackStateCompat.STATE_NONE;
                switch (playbackState) {
                    case SoundWavesPlayerBase.STATE_BUFFERING:
                        state = PlaybackStateCompat.STATE_BUFFERING;
                        break;
                    case SoundWavesPlayerBase.STATE_ENDED:
                        state = PlaybackStateCompat.STATE_NONE;
                        break;
                    case SoundWavesPlayerBase.STATE_IDLE:
                        state = PlaybackStateCompat.STATE_STOPPED;
                        break;
                    case SoundWavesPlayerBase.STATE_READY:
                        state = playWhenReady ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
                        break;
                }

                mPlayerStateManager.updateState(state, getCurrentPosition(), playbackSpeed);

                if (playbackState == STATE_READY) {
                    IEpisode episode = getCurrentItem();
                    if (episode != null) {
                        long duration = getDuration();
                        if (duration != C.TIME_UNSET && episode.setDuration(duration)) {
                            SoundWaves.getAppContext(argContext).getLibraryInstance().updateEpisode(episode);
                        }
                    }
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e(TAG, error.toString());
            }

            @Override
            public void onPositionDiscontinuity() {
            }
        });
    }

    public void addListener(ExoPlayer.EventListener listener) {
        mExoplayer.addListener(listener);
    }

    public void removeListener(ExoPlayer.EventListener listener) {
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

    public void setDataSourceAsync(@NonNull IEpisode argEpisode) throws SecurityException {
        super.setDataSourceAsync(argEpisode);

        try {

            String path = getDataSourceUrl(argEpisode);
            File f = new File(path);
            mIsStreaming = !f.exists();

            if (mIsStreaming) {
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_BUFFERING, startPos, playbackSpeed);
            }

            reset();

            Uri uri = Uri.parse(path);
            setDataSource(mPlayerService, uri);

            setAudioStreamType(PlayerStateManager.AUDIO_STREAM);

            this.isPreparingMedia = true;
        } catch (IOException ex) {
            // TODO: notify the user why the file couldn't be opened
            mIsInitialized = false;
            return;
        } catch (IllegalArgumentException ex) {
            // TODO: notify the user why the file couldn't be opened
            mIsInitialized = false;
            return;
        }

        setOnCompletionListener(completionListener);
        setOnBufferingUpdateListener(bufferListener);
        setOnErrorListener(errorListener);

        seekTo(startPos);

        prepare();

        mIsInitialized = true;

        IEpisode episode = getCurrentItem();
        if (episode != null) {
            start();
            isPreparingMedia = false;
        }
    }

    public boolean isSteaming() { return  mIsStreaming; }

    public void start() {
        mStatus = PlayerStatusObservable.PLAYING;

        // Request audio focus for playback
        int result = mAudioManager.requestAudioFocus(mPlayerService,
                PlayerStateManager.AUDIO_STREAM,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mExoplayer.setPlayWhenReady(true);

            //mPlayerService.notifyStatusChanged();

            trackEventPlay();
        }
    }

    public void stop() {
        if (!isInitialized())
            return;

        mExoplayer.release();
        super.stop();
    }

    @Override
    public void release() {
        super.release();
        mExoplayer.release();
        mIsInitialized = false;
    }

    @Override
    public boolean isPlaying() {
        return mExoplayer.getPlayWhenReady();
    }

    public void rewind() {
        rewind(getCurrentItem());
    }

    public void fastForward() {
        fastForward(getCurrentItem());
    }

    /**
     * Pause the current playing item
     */
    public void pause() {

        mExoplayer.setPlayWhenReady(false);

        mStatus = PlayerStatusObservable.PAUSED;

        MarkAsListenedIfNeeded();
        SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.PAUSE);
    }

    /**
     * Only called when the track is complete. Not when it is stopped or interrupted.
     */
    GenericMediaPlayerInterface.OnCompletionListener completionListener = new GenericMediaPlayerInterface.OnCompletionListener() {
        @Override
        public void onCompletion(GenericMediaPlayerInterface mp) {
            Log.d(TAG, "OnCompletionListener");
            mPlayerStateManager.updateState(PlaybackStateCompat.STATE_STOPPED, 0, playbackSpeed);

            if (!isPreparingMedia)
                mPlayerHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);

            if (mPlayerService == null) {
                return;
            }

            IEpisode item = getCurrentItem();

            if (item == null)
                return;

            if (item instanceof FeedItem) {

                FeedItem feedItem = (FeedItem)item;

                // Delete if required
                boolean doDelete = PreferenceHelper.getBooleanPreferenceValue(mPlayerService,
                        R.string.pref_delete_when_finished_key,
                        R.bool.pref_delete_when_finished_default);

                if (doDelete) {
                    feedItem.delFile(mPlayerService);
                }

                // Mark current as listened
                feedItem.markAsListened();

                SoundWaves.getAppContext(mPlayerService).getLibraryInstance().updateEpisode(feedItem);
            }

            final boolean doPlayNext = PreferenceHelper.getBooleanPreferenceValue(mPlayerService,
                    R.string.pref_continuously_playing_key,
                    R.bool.pref_delete_when_finished_default);


            Handler mainHandler = new Handler(mPlayerService.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    SoundWaves.getAppContext(mContext).getPlaylist().removeFirst();
                    mPlayerService.stop();

                    if (doPlayNext) {
                        mPlayerService.play();
                    }
                }
            };
            mainHandler.post(myRunnable);
        }
    };

    GenericMediaPlayerInterface.OnBufferingUpdateListener bufferListener = new GenericMediaPlayerInterface.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(GenericMediaPlayerInterface mp, int percent) {
            SoundWavesPlayer.this.bufferProgress = percent;
        }
    };


    GenericMediaPlayerInterface.OnErrorListener errorListener = new GenericMediaPlayerInterface.OnErrorListener() {
        @Override
        public boolean onError(GenericMediaPlayerInterface mp, int what, int extra) {
            mStatus = PlayerStatusObservable.STOPPED;
            mPlayerStateManager.updateState(PlaybackStateCompat.STATE_ERROR, startPos, playbackSpeed);
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:

                    mPlayerService.dis_notifyStatus();
                    mIsInitialized = false;
                    release();

                    mPlayerHandler.sendMessageDelayed(PlayerHandler.SERVER_DIED, 2000);
                    return true;
                default:
                    break;
            }
            return errorCallback(mp, what, extra);
        }
    };


    protected boolean errorCallback(GenericMediaPlayerInterface argMp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:

                mPlayerService.dis_notifyStatus();
                mIsInitialized = false;
                release();

                mPlayerHandler.sendMessageDelayed(PlayerHandler.SERVER_DIED, 2000);
                return true;
            default:
                break;
        }
        return false;
    }

    public void setVolume(float vol) {
        mExoplayer.setVolume(vol);;
    }

    public float getVolume() {
        return mExoplayer.getVolume();
    }

    private void MarkAsListenedIfNeeded() {
        if (mPlayerService == null)
            return;

        IEpisode iepisode = getCurrentItem();

        FeedItem episode;
        if (iepisode instanceof FeedItem) {
            episode = (FeedItem)iepisode;
        } else {
            return;
        }

        float playerPosition = (float) getCurrentPosition();

        long episodeDuration = episode.getDuration();

        if (episodeDuration < 0 || playerPosition < 0)
            return;

        float progressPercent = playerPosition / episodeDuration;
        double msLeft = episodeDuration - playerPosition;
        double minLeft = msLeft / 1000 / 60;

        boolean ratioThreshold   = progressPercent >= MARK_AS_LISTENED_RATIO_THRESHOLD;
        boolean minutesThreshold = minLeft < MARK_AS_LISTENED_MINUTES_LEFT_THRESHOLD;

        if (ratioThreshold || minutesThreshold) {
            episode.markAsListened();
        }

        SoundWaves.getAppContext(mPlayerService).getLibraryInstance().updateEpisode(episode);
    }

    @Override
    public float getCurrentPitchStepsAdjustment() {
        return 0;
    }


    public boolean doAutomaticGainControl() {
        return false; //mExoplayer.doAutomaticGainControl();
    }

    public void setAutomaticGainControl(boolean argSetAutomaticGainControl) {
        //mExoplayer.setAutomaticGainControl(argSetAutomaticGainControl);
    }

    public boolean doRemoveSilence() {
        return false; //mExoplayer.doRemoveSilence();
    }

    public void setRemoveSilence(boolean argDoRemoveSilence) {
        //mExoplayer.setRemoveSilence(argDoRemoveSilence);
    }

    @Override
    public long getCurrentPosition() {
        long currentPosition = mExoplayer.getCurrentPosition();
        return currentPosition;
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
        return 0;
    }

    @Override
    public float getMinSpeedMultiplier() {
        return 0;
    }

    @Override
    public void prepare() {
        //mExoplayer.prepare();
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException {
        ExoPlayerMediaSourceHelper mediaSourceHelper = new ExoPlayerMediaSourceHelper(context);
        MediaSource source = mediaSourceHelper.buildMediaSource(uri);
        mExoplayer.prepare(source);
    }

    @Override
    public void reset() {
    }

    @Override
    public long seekTo(long msec) throws IllegalStateException {
        long position = mExoplayer.getCurrentPosition();;
        mExoplayer.seekTo(msec);

        PlayerService ps = mPlayerService;
        if (ps != null) {
            mPlayerService.updateChapter(position);
        }

        return msec;
    }

    @Override
    public void setAudioStreamType(int streamtype) {
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void setPlaybackSpeed(float speed) {
        mExoplayer.setPlaybackSpeed(speed);
    }

    public void startAndFadeIn() {
        super.startAndFadeIn();
        mPlayerHandler.sendEmptyMessageDelayed(PlayerHandler.FADEIN, 10);
    }

    public void FadeOutAndStop(int argDelayMs) {
        super.FadeOutAndStop(argDelayMs);
        cancelFadeOut();
        mPlayerHandler.sendEmptyMessageDelayed(PlayerHandler.FADEOUT, argDelayMs);
    }

    public void cancelFadeOut() {
        mPlayerHandler.removeCallbacks(PlayerHandler.FADEOUT);
    }
}
