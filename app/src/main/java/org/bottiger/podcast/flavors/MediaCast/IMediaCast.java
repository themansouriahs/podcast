package org.bottiger.podcast.flavors.MediaCast;

import org.bottiger.podcast.provider.FeedItem;

/**
 * Created by apl on 11-04-2015.
 */
public interface IMediaCast {

    public void connect();
    public void disconnect();
    public boolean isConnected();

    public boolean loadEpisode(FeedItem argEpisode);

    public void play();
    public void pause();
    public void seekTo(long argPositionMs);
}
