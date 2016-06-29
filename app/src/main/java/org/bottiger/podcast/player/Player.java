package org.bottiger.podcast.player;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by aplb on 28-06-2016.
 */

public interface Player  {

    void setDataSourceAsync(@NonNull String path, int startPos);
    @PlayerStatusObservable.PlayerStatus int getStatus();
    boolean isSteaming();
    boolean isInitialized();

    void toggle();
    void start();
    void stop();
    void release();
    boolean isPlaying();

    long getCurrentPosition();

    void rewind(@Nullable IEpisode argItem);
    void fastForward(@Nullable IEpisode argItem);

    void pause();

    long duration();
    long position();

    long seek(long whereto);
    void setVolume(float vol);

    boolean isCasting();

}
