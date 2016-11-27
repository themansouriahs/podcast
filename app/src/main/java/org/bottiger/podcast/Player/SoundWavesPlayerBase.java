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
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.cloud.EventLogger;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.player.exoplayer.ExtractorRendererBuilder;
import org.bottiger.podcast.player.exoplayer.PodcastAudioRendererV21;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.base.BaseEpisode;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.PlayerService;

import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.rxbus.RxBasicSubscriber;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arvid on 8/27/2015.
 */
public abstract class SoundWavesPlayerBase implements GenericMediaPlayerInterface {

    private static final String TAG = SoundWavesPlayerBase.class.getSimpleName();

    // Constants pulled into this class for convenience.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_ENDED})
    public @interface PlayerState {}
    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    @PlayerStatusObservable.PlayerStatus int mStatus;

    @Nullable
    PlayerService mPlayerService;
    PlayerStateManager mPlayerStateManager;

    @javax.annotation.Nullable
    private NotificationPlayer mNotificationPlayer;

    @NonNull protected Context mContext;

    // AudioManager
    AudioManager mAudioManager;
    private ComponentName mControllerComponentName;

    boolean mIsInitialized = false;

    protected long startPos = 0;

    public SoundWavesPlayerBase(@NonNull Context argContext) {
        mContext = argContext;

        SoundWaves.getRxBus2()
                .toFlowableCommon()
                .ofType(BaseEpisode.SeekEvent.class)
                .subscribe(new RxBasicSubscriber<BaseEpisode.SeekEvent>() {
                    @Override
                    public void onNext(BaseEpisode.SeekEvent seekEvent) {
                        seekTo(seekEvent.getMs());
                    }
                });
    }

    public void setPlayerService(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
        mPlayerStateManager = argPlayerService.getPlayerStateManager();
        this.mControllerComponentName = new ComponentName(mPlayerService,
                HeadsetReceiver.class);
        this.mAudioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setDataSourceAsync(@NonNull IEpisode argEpisode) {
        startPos = getStartPosition(mContext, argEpisode);
        setState(PlayerStatusObservable.PREPARING);
    }

    public void start() {
        setState(PlayerStatusObservable.PLAYING);
    }

    public void stop() {
        mIsInitialized = false;
        mPlayerService.stopForeground(true);
        setState(PlayerStatusObservable.STOPPED);
    }

    protected void setState(@PlayerStatusObservable.PlayerStatus int argState) {

        mStatus = argState;
        switch (argState) {
            case PlayerStatusObservable.PAUSED:
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition(), getCurrentSpeedMultiplier());
                break;
            case PlayerStatusObservable.PLAYING:
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), getCurrentSpeedMultiplier());
                break;
            case PlayerStatusObservable.PREPARING:
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_BUFFERING, startPos, getCurrentSpeedMultiplier());
                break;
            case PlayerStatusObservable.STOPPED:
                mPlayerStateManager.updateState(PlaybackStateCompat.STATE_STOPPED, getCurrentPosition(), getCurrentSpeedMultiplier());
                break;
        }

        mPlayerService.notifyStatusChanged();
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

    public void updateNotificationPlayer() {
        IEpisode episode = PlayerService.getCurrentItem();
        if (episode != null) {
            if (mNotificationPlayer == null && mPlayerService != null) {
                try {
                    mNotificationPlayer = new NotificationPlayer(mPlayerService, episode);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            if (mNotificationPlayer.isShowing() || isPlaying()) {
                mNotificationPlayer.setPlayerService(mPlayerService);
                mNotificationPlayer.show(episode);
            }
        }
    }

    public void removeNotificationPlayer() {
        if (mNotificationPlayer != null)
            mNotificationPlayer.hide();
    }

    public void fastForward(@Nullable IEpisode argItem) {
        if (mPlayerService == null)
            return;

        if (argItem == null)
            return;

        String fastForwardAmount = PreferenceHelper.getStringPreferenceValue(mPlayerService, R.string.pref_player_forward_amount_key, R.string.player_fast_forward_default);
        long seekTo = mPlayerService.position() + Integer.parseInt(fastForwardAmount)*1000; // to ms

        seekTo(seekTo);
    }

    public void rewind(@Nullable IEpisode argItem) {
        if (mPlayerService == null)
            return;

        if (argItem == null)
            return;

        String rewindAmount = PreferenceHelper.getStringPreferenceValue(mPlayerService, R.string.pref_player_backward_amount_key, R.string.player_rewind_default);
        long seekTo = mPlayerService.position() - Integer.parseInt(rewindAmount)*1000; // to ms


        seekTo(seekTo);
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isCasting() {
        return false;
    }
    public abstract void removeListener(ExoPlayer.EventListener listener);

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
        Log.d(TAG, "setOnErrorListener");
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        Log.d(TAG, "setOnCompletionListener");
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        Log.d(TAG, "setOnBufferingUpdateListener");
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

    void trackEventPlay() {
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

    static long getStartPosition(@NonNull Context argContext, @NonNull IEpisode argEpisode) {
        long episodeOffset = argEpisode.getOffset();
        long offset = Math.max(episodeOffset, 0);

        ISubscription subscription = argEpisode.getSubscription(argContext);
        if (subscription != null && subscription.doSkipIntro()) {
            long startSkipAmount = PreferenceHelper.getLongPreferenceValue(argContext, R.string.pref_skip_intro_key, R.integer.skip_into_default);
            offset = Math.max(offset, startSkipAmount);
        }

        return offset;
    }

    static String getDataSourceUrl(@NonNull IEpisode argEpisode) throws SecurityException {
        String dataSource = argEpisode.getURL();
        boolean isFeedItem = argEpisode instanceof FeedItem;

        try {
            if (argEpisode.isDownloaded() && isFeedItem) {
                FeedItem feedItem = (FeedItem) argEpisode;
                String path = feedItem.getAbsolutePath();
                File file = new File(path);
                if (file.exists()) {
                    dataSource = path;
                }
            }
        } catch (IOException ignored) {
        }

        return dataSource;
    }

    @Nullable
    public IEpisode getEpisode() {
        return PlayerService.getCurrentItem();
    }
}
