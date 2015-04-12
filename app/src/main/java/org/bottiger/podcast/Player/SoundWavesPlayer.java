package org.bottiger.podcast.Player;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.flavors.MediaCast.IMediaRouteStateListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;

import java.io.File;
import java.io.IOException;

/**
 * Created by apl on 20-01-2015.
 */
public class SoundWavesPlayer extends MediaPlayer implements IMediaRouteStateListener {

    private static final float MARK_AS_LISTENED_RATIO_THRESHOLD = 0.9f;
    private static final float MARK_AS_LISTENED_MINUTES_LEFT_THRESHOLD = 5f;

    private static final boolean DELETE_WHEN_FINISHED_DEFAULT = false;

    private PlayerService mPlayerService;
    private Handler mHandler;
    private boolean mIsInitialized = false;
    private boolean mIsStreaming = false;

    // AudioManager
    private AudioManager mAudioManager;
    private ComponentName mControllerComponentName;

    // GoogleCast
    private IMediaCast mMediaCast;

    private boolean isPreparingMedia = false;

    @NonNull
    private SharedPreferences mSharedpreferences;

    int bufferProgress = 0;

    int startPos = 0;

    public SoundWavesPlayer(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
        this.mControllerComponentName = new ComponentName(mPlayerService,
                HeadsetReceiver.class);
        this.mAudioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
        mSharedpreferences = PreferenceManager.getDefaultSharedPreferences(mPlayerService.getApplicationContext());
    }

    public void setDataSourceAsync(String path, int startPos) {

        if (isCasting()) {
            mMediaCast.loadEpisode(mPlayerService.getCurrentItem());
            start();
            return;
        }

        try {

            File f = new File(path);
            mIsStreaming = !f.exists();

            reset();
            setDataSource(path);
            setAudioStreamType(AudioManager.STREAM_MUSIC);

            this.startPos = startPos;
            this.isPreparingMedia = true;

            setOnPreparedListener(preparedlistener);
            prepareAsync();
        } catch (IOException ex) {
            // TODO: notify the user why the file couldn't be opened
            mIsInitialized = false;
            return;
        } catch (IllegalArgumentException ex) {
            // TODO: notify the user why the file couldn't be opened
            mIsInitialized = false;
            return;
        }
        setOnCompletionListener(listener);
        setOnBufferingUpdateListener(bufferListener);
        setOnErrorListener(errorListener);

        mIsInitialized = true;
    }

    public boolean isSteaming() { return  mIsStreaming; }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public void toggle() {
        if (isPlaying())
            pause();
        else
            start();
    }

    public void start() {

        if (isCasting()) {
            mMediaCast.play(0);
            PlayerStatusObservable
                    .updateStatus(PlayerStatusObservable.STATUS.PLAYING);
            return;
        }

        // Request audio focus for playback
        int result = mAudioManager.requestAudioFocus(mPlayerService,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioManager
                    .registerMediaButtonEventReceiver(mControllerComponentName);
            super.start();

            PlayerStatusObservable
                    .updateStatus(PlayerStatusObservable.STATUS.PLAYING);
            SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.PLAY);
        }
    }

    public void stop() {

        if (isCasting()) {
            mMediaCast.stop();
            return;
        } else {
            super.reset();
        }

        mIsInitialized = false;
        mPlayerService.stopForeground(true);
        PlayerStatusObservable
                .updateStatus(PlayerStatusObservable.STATUS.STOPPED);
    }

    public void release() {
        mPlayerService.dis_notifyStatus();
        super.stop();
        super.release();
        mAudioManager
                .unregisterMediaButtonEventReceiver(mControllerComponentName);
        mIsInitialized = false;
    }

    @Override
    public boolean isPlaying() {
        return super.isPlaying() || isPlayingUsingMediaRoute();
    }

    public boolean isPlayingUsingMediaRoute() {
        return mMediaCast != null && mMediaCast.isPlaying();
    }

    @Override
    public int getCurrentPosition() {
        return mMediaCast != null && mMediaCast.isActive() ? mMediaCast.getCurrentPosition() : super.getCurrentPosition();
    }

    public void rewind(FeedItem argItem) {
        if (mPlayerService == null)
            return;

        argItem.setPosition(mPlayerService.getContentResolver(), 0);

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(0);
        }
    }

    public void fastForward(FeedItem argItem) {
        if (mPlayerService == null)
            return;

        String fastForwardAmount = mSharedpreferences.getString("pref_player_forward_amount", "60");
        long seekTo = mPlayerService.position() + Integer.parseInt(fastForwardAmount)*1000; // to ms

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(seekTo);
        }

        argItem.setPosition(mPlayerService.getContentResolver(), seekTo);
    }

    /**
     * Pause the current playing item
     */
    public void pause() {

        if (isCasting()) {
            mMediaCast.stop();
            return;
        } else {
            super.pause();
        }

        MarkAsListenedIfNeeded();
        PlayerStatusObservable
                .updateStatus(PlayerStatusObservable.STATUS.PAUSED);
        SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.PAUSE);
    }

    @Deprecated
    public int getBufferProgress() {
        return this.bufferProgress;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            FeedItem item = mPlayerService.getCurrentItem();

            if (item != null) {

                // Mark current as listened
                item.markAsListened();

               if (mPlayerService != null) {
                   // Delete if required
                    Resources resources = mPlayerService.getResources();
                    ContentResolver resolver = mPlayerService.getContentResolver();
                    boolean doDelete = mSharedpreferences.getBoolean(resources.getString(R.string.pref_delete_when_finished_key), DELETE_WHEN_FINISHED_DEFAULT);

                    if (doDelete) {
                        item.delFile(resolver);

                    }

                   item.update(resolver);
                }
            }

            if (!isPreparingMedia)
                mHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);

        }
    };

    MediaPlayer.OnBufferingUpdateListener bufferListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            SoundWavesPlayer.this.bufferProgress = percent;
        }
    };

    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.seekTo(startPos);
            mPlayerService.getCurrentItem().setDuration(mp.getDuration(), false);
            start();
            isPreparingMedia = false;
        }
    };

    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:

                    mPlayerService.dis_notifyStatus();
                    mIsInitialized = false;
                    release();

                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(PlayerHandler.SERVER_DIED), 2000);
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    public long duration() {
        return getDuration();
    }

    public long position() {
        return getCurrentPosition();
    }

    public long seek(long whereto) {
        if (isCasting()) {
            mMediaCast.seekTo(whereto);
        } else {
            seekTo((int) whereto);
        }
        return whereto;
    }

    public void setVolume(float vol) {
        setVolume(vol, vol);
    }

    private void MarkAsListenedIfNeeded() {
        if (mPlayerService == null)
            return;

        FeedItem episode = mPlayerService.getCurrentItem();
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

        episode.update(mPlayerService.getContentResolver());
    }

    @Override
    public void onStateChanged(IMediaCast castProvider) {
        if (castProvider == null)
            return;

        mMediaCast = castProvider;

        if (mPlayerService == null)
            return;

        FeedItem episode = mPlayerService.getCurrentItem();

        if (episode == null)
            return;

        if (mMediaCast.isConnected()) {
            mMediaCast.loadEpisode(episode);
            mMediaCast.seekTo(episode.offset);

            if (super.isPlaying()) {
                int offst = super.getCurrentPosition();
                super.pause();
                mMediaCast.play(offst);
            }

            PlayerStatusObservable
                    .updateStatus(PlayerStatusObservable.STATUS.PLAYING);
        }
    }

    public boolean isCasting() {
        return mMediaCast != null && mMediaCast.isConnected();
    }
}
