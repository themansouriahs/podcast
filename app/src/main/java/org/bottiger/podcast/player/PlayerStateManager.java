package org.bottiger.podcast.player;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ImageLoaderUtils;

/**
 * Created by apl on 12-02-2015.
 *
 * http://stackoverflow.com/questions/28124708/android-notification-mediastyle-not-responding-to-mediasession-updates
 *
 */
public class PlayerStateManager extends MediaSessionCompat.Callback {

    private static final String TAG = PlayerStateManager.class.getSimpleName();
    private static final String SESSION_TAG = "SWMediaSession";

    public static int AUDIO_STREAM = AudioManager.STREAM_MUSIC;

    public static final String ACTION_TOGGLE = "playpause_action";
    public static final String ACTION_TOGGLE_MUTE = "mute_toggle_action";

    private MediaSessionCompat mSession;
    private PlayerService mPlayerService;

    @PlaybackStateCompat.State private int mCurrentState = PlaybackStateCompat.STATE_NONE;

    /**
     * Started when the PlayerService is started
     */
    public PlayerStateManager() {
        Log.d(TAG, "started");
    }

    public void setService(@NonNull PlayerService argService) {
        Log.d(TAG, "setting service");
        mPlayerService = argService;

        ComponentName mediaButtonReceiver = new ComponentName(mPlayerService, HeadsetReceiver.class);
        mSession = new MediaSessionCompat(mPlayerService, SESSION_TAG, mediaButtonReceiver, null);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Intent toggleIntent = new Intent(NotificationPlayer.toggleAction);
        PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(mPlayerService, 0, toggleIntent, 0);
        mSession.setMediaButtonReceiver(pendingToggleIntent);

        mSession.setCallback(this);
        mSession.setActive(true);

    }

    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     */
    public void onPlay() {
        Log.d(TAG, "onPlay");

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        mPlayerService.startService(new Intent(mPlayerService.getApplicationContext(), PlayerService.class));
    }


    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     */
    public void onStop() {
        Log.d(TAG, "onStop");
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        //mDelayedStopHandler.removeCallbacksAndMessages(null);
        //mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        mPlayerService.dis_notifyStatus();
        mPlayerService.stopForeground(true);
    }

    public void onSkipToNext() {
        Log.d(TAG, "skipToNext");
        mPlayerService.playNext();
    }

    @Override
    public void onFastForward() {
        mPlayerService.getPlayer().fastForward(null);
        mPlayerService.notifyStatusChanged();
    }

    @Override
    public void onRewind() {
        mPlayerService.getPlayer().rewind(null);
        mPlayerService.notifyStatusChanged();
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        Log.d(TAG, "received action: " + action); // NoI18N

        if (ACTION_TOGGLE.equals(action)) {
            mPlayerService.toggle();
        }

        if (ACTION_TOGGLE_MUTE.equals(action)) {
            if (Build.VERSION.SDK_INT >= 23) {
                AudioManager audioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AUDIO_STREAM, AudioManager.ADJUST_TOGGLE_MUTE, 0);
                mPlayerService.notifyStatusChanged();
            }
        }
    }

    public void release() {
        mSession.release();
    }

    public MediaSessionCompat.Token getToken() {
        return mSession.getSessionToken();
    }

    public void updateMedia(@NonNull IEpisode argEpisode) {
        Log.d(TAG, "Update media: episode: " + argEpisode); // NoI18N

        final MediaMetadataCompat.Builder mMetaBuilder = new MediaMetadataCompat.Builder();
        populateFastMediaMetadata(mMetaBuilder, argEpisode);

        int bitmapSize = 512;

        String url = argEpisode.getArtwork(mPlayerService);
        if (url == null) {
            mSession.setMetadata(mMetaBuilder.build());
            return;
        }

        ImageLoaderUtils.getGlide(mPlayerService, url)
                .into(new SimpleTarget<Bitmap>(bitmapSize, bitmapSize) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        mMetaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource);
                        mMetaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, resource);
                        mMetaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, resource);
                        mSession.setMetadata(mMetaBuilder.build());
                    }
                });
    }

    void updateState(@PlaybackStateCompat.State int argState, long argPosition, float argPlaybackSpeed) {
        Log.d(TAG, "Update State:" + argState); // NoI18N

        PlaybackStateCompat.Builder stateBuilder = getPlaybackState(argState, argPosition, argPlaybackSpeed);
        mSession.setPlaybackState(stateBuilder.build());

        if (mCurrentState != argState) {
            mCurrentState = argState;
            mPlayerService.notifyStatusChanged();
        }
    }

    public MediaSessionCompat getSession() {
        return mSession;
    }

    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        Log.d(TAG, "onMediaButtonEvent");
        KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        boolean wasHandled = HeadsetReceiver.handleMediaButtonEvent(event, mPlayerService);
        return wasHandled;
    }

    private void populateFastMediaMetadata(@NonNull MediaMetadataCompat.Builder mMetaBuilder, @NonNull IEpisode argEpisode) {
        ISubscription subscription = argEpisode.getSubscription(mPlayerService);
        String author = !TextUtils.isEmpty(argEpisode.getAuthor()) ? argEpisode.getAuthor() : subscription.getTitle();

        mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, argEpisode.getURL());
        mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, argEpisode.getTitle());
        mMetaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, argEpisode.getDuration());
        mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, author);
        mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, argEpisode.getArtwork(mPlayerService));

        if (subscription != null)
            mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, subscription.getTitle());
    }

    private PlaybackStateCompat.Builder getPlaybackState(@PlaybackStateCompat.State int argState, long argPosition, float argPlaybackSpeed) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();

        int queueId = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueId = MediaSession.QueueItem.UNKNOWN_ID;
        }
        stateBuilder.setActiveQueueItemId(queueId);

        long actions =  PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        if (argState == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }

        stateBuilder.setActions(actions);
        //stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f);
        stateBuilder.setState(argState, argPosition, argPlaybackSpeed, SystemClock.elapsedRealtime());

        return stateBuilder;
    }

    @PlaybackStateCompat.State
    public int getState() {
        return mCurrentState;
    }
}
