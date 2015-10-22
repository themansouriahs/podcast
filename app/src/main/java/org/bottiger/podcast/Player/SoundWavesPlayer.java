package org.bottiger.podcast.player;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.flavors.MediaCast.IMediaRouteStateListener;
import org.bottiger.podcast.listeners.PlayerStatusData;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;

import java.io.File;
import java.io.IOException;

/**
 * Created by apl on 20-01-2015.
 */
public class SoundWavesPlayer extends org.bottiger.podcast.player.SoundWavesPlayerBase implements IMediaRouteStateListener {

    private static final float MARK_AS_LISTENED_RATIO_THRESHOLD = 0.9f;
    private static final float MARK_AS_LISTENED_MINUTES_LEFT_THRESHOLD = 5f;

    private static final boolean DELETE_WHEN_FINISHED_DEFAULT = false;

    private final String PLAYER_ACTION_FASTFORWARD_KEY;
    private final String PLAYER_ACTION_REWIND_KEY;

    // needs to be a string
    private final String PLAYER_ACTION_FASTFORWARD_DEFAULT_VALUE = "60";
    private final String PLAYER_ACTION_REWIND_DEFAULT_VALUE = "60";

    private @PlayerStatusObservable.PlayerStatus int mStatus;

    private PlayerService mPlayerService;
    private org.bottiger.podcast.player.PlayerHandler mHandler;
    private org.bottiger.podcast.player.PlayerStateManager mPlayerStateManager;

    private boolean mIsInitialized = false;
    private boolean mIsStreaming = false;

    // AudioManager
    private AudioManager mAudioManager;
    private ComponentName mControllerComponentName;

    // GoogleCast
    private IMediaCast mMediaCast;

    private PlayerStatusObservable mPlayerStatusObservable;

    private boolean isPreparingMedia = false;

    @NonNull
    private SharedPreferences mSharedpreferences;

    int bufferProgress = 0;
    int startPos = 0;
    float playbackSpeed = 1.0f;

    public SoundWavesPlayer(@NonNull PlayerService argPlayerService) {
        super(argPlayerService);
        mPlayerService = argPlayerService;
        mPlayerStateManager = argPlayerService.getPlayerStateManager();
        this.mControllerComponentName = new ComponentName(mPlayerService,
                HeadsetReceiver.class);
        this.mAudioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
        mSharedpreferences = PreferenceManager.getDefaultSharedPreferences(mPlayerService.getApplicationContext());

        Resources resources = argPlayerService.getResources();
        PLAYER_ACTION_FASTFORWARD_KEY = resources.getString(R.string.pref_player_forward_amount_key);
        PLAYER_ACTION_REWIND_KEY = resources.getString(R.string.pref_player_backward_amount_key);

        mPlayerStatusObservable = new PlayerStatusObservable();
        SoundWaves.getBus().register(mPlayerStatusObservable); // FIXME is never unregistered!!!
    }

    public void setDataSourceAsync(String path, int startPos) {

        mStatus = PlayerStatusObservable.PREPARING;
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_CONNECTING, startPos, playbackSpeed);

        if (isCasting()) {
            mMediaCast.loadEpisode(mPlayerService.getCurrentItem());
            start();
            mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PLAYING, startPos, playbackSpeed);
            return;
        }

        try {

            File f = new File(path);
            mIsStreaming = !f.exists();

            if (mIsStreaming) {
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_BUFFERING, startPos, playbackSpeed);
            }

            reset();

            Uri uri = Uri.parse(path);
            setDataSource(mPlayerService, uri);
            /*
            if (mIsStreaming)
                setDataSource(path);
            else {
                //FileInputStream fis = new FileInputStream(path);
                //FileDescriptor fd = fis.getFD();
                //setDataSource(fd);
                setDataSource(path);
            }*/
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

    public @PlayerStatusObservable.PlayerStatus int getStatus() {
        return mStatus;
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

        mStatus = PlayerStatusObservable.PLAYING;
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), playbackSpeed);

        PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.PLAYING);

        // If we are using a Chromecats we send the file to it
        if (isCasting()) {
            mMediaCast.play(0);
            SoundWaves.getBus().post(psd);
            return;
        }

        // Request audio focus for playback
        int result = mAudioManager.requestAudioFocus(mPlayerService,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            super.start();

            SoundWaves.getBus().post(psd);
            SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.PLAY);
        }
    }

    public void stop() {
        if (!isInitialized())
            return;

        if (isCasting()) {
            mMediaCast.stop();
            return;
        } else {
            super.reset();
        }

        mStatus = PlayerStatusObservable.STOPPED;
        mIsInitialized = false;
        mPlayerService.stopForeground(true);
        PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.STOPPED);
        SoundWaves.getBus().post(psd);
    }

    public void release() {
        mPlayerService.dis_notifyStatus();
        super.stop();
        super.release();
        mAudioManager
                .unregisterMediaButtonEventReceiver(mControllerComponentName);
        mIsInitialized = false;
        mStatus = PlayerStatusObservable.STOPPED;
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

    public void rewind(@Nullable IEpisode argItem) {
        if (mPlayerService == null)
            return;

        if (argItem == null)
            return;

        String rewindAmount = mSharedpreferences.getString(PLAYER_ACTION_REWIND_KEY, PLAYER_ACTION_REWIND_DEFAULT_VALUE);
        long seekTo = mPlayerService.position() - Integer.parseInt(rewindAmount)*1000; // to ms

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(seekTo);
        }
    }

    public void fastForward(@Nullable IEpisode argItem) {
        if (mPlayerService == null)
            return;

        if (argItem == null)
            return;

        String fastForwardAmount = mSharedpreferences.getString(PLAYER_ACTION_FASTFORWARD_KEY, PLAYER_ACTION_FASTFORWARD_DEFAULT_VALUE);
        long seekTo = mPlayerService.position() + Integer.parseInt(fastForwardAmount)*1000; // to ms

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(seekTo);
        }
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

        mStatus = PlayerStatusObservable.PAUSED;

        MarkAsListenedIfNeeded();
        PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.PAUSED);
        SoundWaves.getBus().post(psd);
        SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.PAUSE);
    }

    @Deprecated
    public int getBufferProgress() {
        return this.bufferProgress;
    }

    public void setHandler(PlayerHandler handler) {
        mHandler = handler;
    }

    GenericMediaPlayerInterface.OnCompletionListener listener = new GenericMediaPlayerInterface.OnCompletionListener() {
        @Override
        public void onCompletion(GenericMediaPlayerInterface mp) {
            mPlayerStateManager.updateState(PlaybackStateCompat.STATE_STOPPED, 0, playbackSpeed);
            IEpisode item = mPlayerService.getCurrentItem();

            if (item != null && item instanceof FeedItem) {

                FeedItem feedItem = (FeedItem)item;
                // Mark current as listened
                feedItem.markAsListened();

               if (mPlayerService != null) {
                   // Delete if required
                    Resources resources = mPlayerService.getResources();
                    ContentResolver resolver = mPlayerService.getContentResolver();
                    boolean doDelete = mSharedpreferences.getBoolean(resources.getString(R.string.pref_delete_when_finished_key), DELETE_WHEN_FINISHED_DEFAULT);

                    if (doDelete) {
                        feedItem.delFile(mPlayerService);

                    }

                   SoundWaves.getLibraryInstance().updateEpisode(feedItem);
                }
            }

            if (!isPreparingMedia)
                mHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);

        }
    };

    GenericMediaPlayerInterface.OnBufferingUpdateListener bufferListener = new GenericMediaPlayerInterface.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(GenericMediaPlayerInterface mp, int percent) {
            SoundWavesPlayer.this.bufferProgress = percent;
        }
    };


    GenericMediaPlayerInterface.OnPreparedListener preparedlistener = new GenericMediaPlayerInterface.OnPreparedListener() {
        @Override
        public void onPrepared(GenericMediaPlayerInterface mp) {
            mp.seekTo(startPos);
            mPlayerService.getCurrentItem().setDuration(mp.getDuration());
            start();
            isPreparingMedia = false;
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

        SoundWaves.getLibraryInstance().updateEpisode(episode);
    }

    @Override
    public void onStateChanged(IMediaCast castProvider) {
        if (castProvider == null)
            return;

        mMediaCast = castProvider;

        if (mPlayerService == null)
            return;

        IEpisode episode = mPlayerService.getCurrentItem();

        if (episode == null)
            return;

        if (mMediaCast.isConnected()) {
            mMediaCast.loadEpisode(episode);

            if (episode instanceof FeedItem) {
                mMediaCast.seekTo(((FeedItem)episode).offset);
            }

            if (super.isPlaying()) {
                int offst = super.getCurrentPosition();
                super.pause();
                mMediaCast.play(offst);
            }

            mStatus = PlayerStatusObservable.PLAYING;
            PlayerStatusData psd = new PlayerStatusData(mPlayerService.getCurrentItem(), PlayerStatusObservable.PLAYING);
            SoundWaves.getBus().post(psd);
        }
    }

    public boolean isCasting() {
        return mMediaCast != null && mMediaCast.isConnected();
    }
}
