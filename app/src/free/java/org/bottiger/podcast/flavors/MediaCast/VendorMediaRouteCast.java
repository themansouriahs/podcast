package org.bottiger.podcast.flavors.MediaCast;

import android.support.v7.media.MediaRouteSelector;

import org.bottiger.podcast.MediaRouterPlaybackActivity;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 12-04-2015.
 */
public class VendorMediaRouteCast implements IMediaCast {
    
    public VendorMediaRouteCast(MediaRouterPlaybackActivity mediaRouterPlaybackActivity) {
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean loadEpisode(IEpisode argEpisode) {
        return false;
    }

    @Override
    public void play(long argStartPosition) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(long argPositionMs) {

    }

    @Override
    public void registerStateChangedListener(IMediaRouteStateListener argListener) {

    }

    @Override
    public void unregisterStateChangedListener(IMediaRouteStateListener argListener) {

    }

    public void startDiscovery() {
    }

    public void stopDiscovery() {
    }

    public MediaRouteSelector getRouteSelector() {
        return null;
    }
}
