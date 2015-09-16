package org.bottiger.podcast;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.bottiger.podcast.flavors.MediaCast.VendorMediaRouteCast;
import org.bottiger.podcast.listeners.DownloadProgress;
import org.bottiger.podcast.service.PlayerService;

/**
 * Created by apl on 28-03-2015.
 */
public class MediaRouterPlaybackActivity extends ToolbarActivity {

    private static final String TAG = "MediaRouterPlayback";

    private MediaRouterEventReciever mMediaRouterEventReciever;
    protected VendorMediaRouteCast mMediaRouteCast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaRouteCast = new VendorMediaRouteCast(this);
        mMediaRouteCast.startDiscovery();
        mMediaRouterEventReciever = new MediaRouterEventReciever();
    }

    @Override
    protected void onDestroy() {
        mMediaRouteCast.stopDiscovery();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        SoundWaves.getBus().register(mMediaRouterEventReciever);
        super.onResume();
    }

    @Override
    protected void onPause() {
        SoundWaves.getBus().unregister(mMediaRouterEventReciever);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MediaRouteSelector mediaRouteSelector = mMediaRouteCast.getRouteSelector();

        if (mediaRouteSelector != null) {
            // Inflate the menu and configure the media router action provider.
            getMenuInflater().inflate(R.menu.media_router, menu);

            // Attach the MediaRouteSelector to the menu item
            MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
            MediaRouteActionProvider mediaRouteActionProvider =
                    (MediaRouteActionProvider) MenuItemCompat.getActionProvider(
                            mediaRouteMenuItem);
            mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
        }

        // Return true to show the menu.
        return true;
    }

    /**
     * Hack, sort of.
     *
     * This class was introduced because otto (for performance reasons) does not look at sub/super-types of input objects.
     * If I register this class like this: SoundWaves.getBus().register(this);
     *
     * "this" will not be of the type "MediaRouterPlaybackActivity" but "MainActivity". Therefore the @Subscribe method would
     * not be called here, but need to be located in the MainActivity - where it does not belong.
     */
    class MediaRouterEventReciever {

        @Subscribe
        public void playerServiceBound(@NonNull SoundWaves.PlayerServiceBound argPlayerServiceBound) {
            if (argPlayerServiceBound.isConnected) {
                SoundWaves.sBoundPlayerService.setMediaCast(mMediaRouteCast);
            }
        }

        @Produce
        public SoundWaves.PlayerServiceBound producePlayerService() {
            PlayerService playerService = SoundWaves.sBoundPlayerService;

            SoundWaves.PlayerServiceBound playerServiceBound = new SoundWaves.PlayerServiceBound();
            playerServiceBound.isConnected = false;

            if (playerService != null) {
                playerServiceBound.isConnected = true;
                playerServiceBound(playerServiceBound);
            }
            return playerServiceBound;
        }

    }

}
