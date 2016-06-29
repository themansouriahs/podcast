package org.bottiger.podcast.player;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.TrackRenderer;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.cloud.EventLogger;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.flavors.MediaCast.IMediaRouteStateListener;
import org.bottiger.podcast.listeners.PlayerStatusData;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.player.exoplayer.ExtractorRendererBuilder;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.io.File;
import java.io.IOException;

/**
 * Created by apl on 20-01-2015.
 */
public class SoundWavesPlayer extends org.bottiger.podcast.player.SoundWavesPlayerBase {

    private static final String TAG = "SoundWavesPlayerBase";

    private static final float MARK_AS_LISTENED_RATIO_THRESHOLD = 0.9f;
    private static final float MARK_AS_LISTENED_MINUTES_LEFT_THRESHOLD = 5f;


    private boolean mIsInitialized = false;
    private boolean mIsStreaming = false;

    private PlayerStatusObservable mPlayerStatusObservable;

    private boolean isPreparingMedia = false;

    int bufferProgress = 0;
    int startPos = 0;
    float playbackSpeed = 1.0f;

    private ExoPlayerWrapper mExoplayer;
    private static final int RENDERER_COUNT = 1;

    private TrackRenderer[] mTrackRendere = new TrackRenderer[ExoPlayerWrapper.RENDERER_COUNT];

    public SoundWavesPlayer(@NonNull Context argContext) {
        super(argContext);

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

        mPlayerStatusObservable = new PlayerStatusObservable();
        SoundWaves.getBus().register(mPlayerStatusObservable); // FIXME is never unregistered!!!
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

    public void setDataSourceAsync(String path, int startPos) {
        super.setDataSourceAsync(path, startPos);

        try {

            File f = new File(path);
            mIsStreaming = !f.exists();

            if (mIsStreaming) {
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_BUFFERING, startPos, playbackSpeed);
            }

            reset();

            Uri uri = Uri.parse(path);
            setDataSource(mPlayerService, uri);

            setAudioStreamType(PlayerStateManager.AUDIO_STREAM);

            this.startPos = startPos;
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

        IEpisode episode = PlayerService.getCurrentItem();
        if (episode != null) {
            start();
            isPreparingMedia = false;
        }

        mIsInitialized = true;
    }

    public boolean isSteaming() { return  mIsStreaming; }

    public void start() {
        // Request audio focus for playback
        int result = mAudioManager.requestAudioFocus(mPlayerService,
                PlayerStateManager.AUDIO_STREAM,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mExoplayer.setPlayWhenReady(true);

            super.start();

            if (SoundWaves.sAnalytics == null) {
                VendorCrashReporter.report("sAnalytics null", "In playerService");
            }

            SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.PLAY);

            ISubscription sub = mPlayerService.getCurrentItem().getSubscription(mPlayerService);
            String url = sub != null ? sub.getURLString() : "";
            EventLogger.postEvent(mPlayerService,
                    EventLogger.LISTEN_EPISODE,
                    mPlayerService.getCurrentItem().isDownloaded() ? 1 : null,
                    mPlayerService.getCurrentItem().getURL(),
                    url);

            EventLogger.postEvent(mPlayerService,
                    EventLogger.LISTEN_PODCAST,
                    null,
                    null,
                    url);
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
    }

    @Override
    public boolean isPlaying() {
        return mExoplayer.getPlayWhenReady();
    }

    void rewind() {
        rewind(null);
    }

    public void rewind(@Nullable IEpisode argItem) {
        if (mPlayerService == null)
            return;

        if (argItem == null) {
            argItem = mPlayerService.getCurrentItem();
        }

        if (argItem == null)
            return;

        String rewindAmount = PreferenceHelper.getStringPreferenceValue(mPlayerService, R.string.pref_player_backward_amount_key, R.string.player_rewind_default);
        long seekTo = mPlayerService.position() - Integer.parseInt(rewindAmount)*1000; // to ms

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(seekTo);
        }
    }

    void fastForward() {
        fastForward(null);
    }

    public void fastForward(@Nullable IEpisode argItem) {
        if (mPlayerService == null)
            return;

        if (argItem == null) {
            argItem = mPlayerService.getCurrentItem();
        }

        if (argItem == null)
            return;

        String fastForwardAmount = PreferenceHelper.getStringPreferenceValue(mPlayerService, R.string.pref_player_forward_amount_key, R.string.player_fast_forward_default);
        long seekTo = mPlayerService.position() + Integer.parseInt(fastForwardAmount)*1000; // to ms

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(seekTo);
        }
    }

    /**
     * Pause the current playing item
     */
    public void pause() {

        mExoplayer.setPlayWhenReady(false);

        mStatus = PlayerStatusObservable.PAUSED;
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition(), playbackSpeed);

        MarkAsListenedIfNeeded();
        PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.PAUSED);
        SoundWaves.getBus().post(psd);
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
                mHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);

            if (mPlayerService == null) {
                return;
            }

            IEpisode item = mPlayerService.getCurrentItem();

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
                    mPlayerService.getPlaylist().removeItem(0);
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

                    mHandler.sendMessageDelayed(PlayerHandler.SERVER_DIED, 2000);
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

                mHandler.sendMessageDelayed(PlayerHandler.SERVER_DIED, 2000);
                return true;
            default:
                break;
        }
        return false;
    }

    public long duration() {
        return getDuration();
    }

    public long position() {
        return getCurrentPosition();
    }

    public long seek(long whereto) {
        seekTo((int) whereto);
        return whereto;
    }

    public void setVolume(float vol) {
        setVolume(vol, vol);
    }

    private void MarkAsListenedIfNeeded() {
        if (mPlayerService == null)
            return;

        IEpisode iepisode = mPlayerService.getCurrentItem();

        FeedItem episode = null;
        if (iepisode instanceof FeedItem) {
            episode = (FeedItem)iepisode;
        } else {
            return;
        }

        float playerPosition = (float) getCurrentPosition();

        if (episode == null)
            return;

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
        return 0;
    }

    @Override
    public float getMinSpeedMultiplier() {
        return 0;
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
    public void reset() {
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        mExoplayer.seekTo(msec);
    }

    @Override
    public void setAudioStreamType(int streamtype) {
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void setPlaybackSpeed(float speed) {
        mExoplayer.setPlaybackSpeed(speed);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }
}
