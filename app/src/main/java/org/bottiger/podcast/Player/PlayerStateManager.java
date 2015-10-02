package org.bottiger.podcast.Player;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSession;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.facebook.datasource.DataSource;

import org.bottiger.podcast.images.FrescoHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;

/**
 * Created by apl on 12-02-2015.
 *
 * http://stackoverflow.com/questions/28124708/android-notification-mediastyle-not-responding-to-mediasession-updates
 *
 */
public class PlayerStateManager extends MediaSessionCompat.Callback {

    private static final String TAG = "PlayerStateManager";
    private static final String SESSION_TAG = "SWMediaSession";

    private MediaSessionCompat mSession;
    private PlayerService mPlayerService;

    public PlayerStateManager(@NonNull PlayerService argService) {
        Log.d(TAG, "Constructor");

        mPlayerService = argService;
        ComponentName mediaButtonReceiver = new ComponentName(argService, HeadsetReceiver.class);
        mSession = new MediaSessionCompat(argService, SESSION_TAG, mediaButtonReceiver, null);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setCallback(this);
        mSession.setActive(true);
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

        Glide.with(mPlayerService)
                .load(url)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(bitmapSize, bitmapSize) {
                    @Override
                    public void onResourceReady(Bitmap argBitmap, GlideAnimation anim) {
                        mMetaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, argBitmap);
                        mMetaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, argBitmap);
                        mMetaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, argBitmap);
                        mSession.setMetadata(mMetaBuilder.build());
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        mSession.setMetadata(mMetaBuilder.build());
                    }
                });
    }

    public void updateState(@PlaybackStateCompat.State int argState, long argPosition, float argPlaybackSpeed) {
        Log.d(TAG, "Update State:"); // NoI18N

        PlaybackStateCompat.Builder stateBuilder = getPlaybackState(argState, argPosition, argPlaybackSpeed);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private void populateFastMediaMetadata(@NonNull MediaMetadataCompat.Builder mMetaBuilder, @NonNull IEpisode argEpisode) {
        //Subscription subscription = argEpisode.getSubscription(mPlayerService);

        mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, argEpisode.getTitle());
        //mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, "yo");
        mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, argEpisode.getAuthor());
        //mMetaBuilder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, "ko");
        //mMetaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 3);
        //mMetaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 15);
        //mMetaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, 1);
    }

    private PlaybackStateCompat.Builder getPlaybackState(@PlaybackStateCompat.State int argState, long argPosition, float argPlaybackSpeed) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();

        stateBuilder.setActiveQueueItemId(MediaSession.QueueItem.UNKNOWN_ID);

        long actions =  PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_SEEK_TO |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        stateBuilder.setActions(actions);
        //stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f);
        stateBuilder.setState(argState, argPosition, argPlaybackSpeed);

        return stateBuilder;
    }
}
