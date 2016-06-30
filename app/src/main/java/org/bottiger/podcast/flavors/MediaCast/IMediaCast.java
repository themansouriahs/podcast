package org.bottiger.podcast.flavors.MediaCast;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 11-04-2015.
 */
public interface IMediaCast {

    public void connect();
    public void disconnect();

    public boolean isConnected();
    public boolean isPlaying();
    public boolean isActive();

    public boolean loadEpisode(IEpisode argEpisode);

    public void play(long argStartPosition);
    public void pause();
    public void stop();

    public int getCurrentPosition();
    public void seekTo(long argPositionMs);

    public void registerStateChangedListener(IMediaRouteStateListener argListener);
    public void unregisterStateChangedListener(IMediaRouteStateListener argListener);
}
