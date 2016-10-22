package org.bottiger.podcast.flavors.player.googlecast;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.flavors.MediaCast.IMediaRouteStateListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.Player;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by aplb on 28-06-2016.
 */

public abstract class GoogleCastPlayer extends SoundWavesPlayerBase {


    public GoogleCastPlayer(@NonNull Context argContext) {
        super(argContext);
    }
}
