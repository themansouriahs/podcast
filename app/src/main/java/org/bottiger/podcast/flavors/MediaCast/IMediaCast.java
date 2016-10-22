package org.bottiger.podcast.flavors.MediaCast;

import android.content.Context;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.view.Menu;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 11-04-2015.
 */
public interface IMediaCast {

    //New
    void setupMediaButton(@NonNull Context argContext, Menu menu, @MenuRes int argMenuResource);

    // Old

    public void connect();
    public void disconnect();

    public boolean isConnected();
    public boolean isPlaying();
    public boolean isActive();

    public boolean loadEpisode(IEpisode argEpisode);

    public void play(long argStartPosition);
    public void pause();
    public void stop();

    public void registerStateChangedListener(IMediaRouteStateListener argListener);
    public void unregisterStateChangedListener(IMediaRouteStateListener argListener);

    void onCreate();
    void onResume();
    void onPause();
}
