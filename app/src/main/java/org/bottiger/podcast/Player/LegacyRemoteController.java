package org.bottiger.podcast.Player;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;

/**
 * Created by apl on 11-02-2015.
 *
 * Used for anything below API level 21 (lolipop)
 */
public class LegacyRemoteController {

    private final String TAG = "RemoteController";

    private LegacyRemoteControlClient remoteControlClient;
    private PlayerService mContext;

    @TargetApi(20)
    public void register(PlayerService context)
    {
        mContext = context;

        if (remoteControlClient == null)
        {
            AudioManager audioManager = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);

            ComponentName myEventReceiver = new ComponentName(context.getPackageName(), HeadsetReceiver.class.getName());
            audioManager.registerMediaButtonEventReceiver(myEventReceiver);

            // build the PendingIntent for the remote control client
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(myEventReceiver);
            // create and register the remote control client
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);
            remoteControlClient = new LegacyRemoteControlClient(mediaPendingIntent);
            remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                            | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                            | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                            | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
            );
            audioManager.registerRemoteControlClient(remoteControlClient);
        }
    }

    /**
     * Update the state of the remote control.
     */
    public void updatePlayingState(boolean isPlaying)
    {
        if(remoteControlClient != null)
        {
            if (isPlaying)
            {
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }

            else
            {
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }
        }
    }

    /**
     * Updates the state of the remote control to "stopped".
     */
    public void stop()
    {
        if (remoteControlClient != null)
        {
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        }
    }

    private void updateSimpleMetaData(RemoteControlClient.MetadataEditor editor, IEpisode episode, @Nullable Bitmap argBitmap) {
        editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, (long)1000);
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, episode.getAuthor());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, episode.getTitle());

        if (argBitmap != null && !argBitmap.isRecycled()) {
            //editor.putBitmap(android.media.RemoteController.MetadataEditor.BITMAP_KEY_ARTWORK, argBitmap);
        }

        editor.apply();
    }

    /**
     * Release the remote control.
     */
    public void release() {
        remoteControlClient = null;
    }
}
