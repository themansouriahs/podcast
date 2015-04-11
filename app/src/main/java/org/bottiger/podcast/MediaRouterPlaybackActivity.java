package org.bottiger.podcast;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaSessionStatus;
import android.support.v7.media.RemotePlaybackClient;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.CastMediaControlIntent;

import org.bottiger.podcast.flavors.MediaCast.MediaRouteCast;
import org.bottiger.podcast.flavors.MediaCast.VendorMediaCast;

/**
 * Created by apl on 28-03-2015.
 */
public class MediaRouterPlaybackActivity extends ToolbarActivity {

    private static final String TAG = "MediaRouterPlayback";

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mSelector;
    private VendorMediaCast mMediaCast;

    protected MediaRouteCast mMediaRouteCast;

    private final MediaRouter.Callback mMediaRouterCallback =
            new MediaRouter.Callback() {

                @Override
                public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "onRouteSelected: route=" + route);

                    mMediaCast = new VendorMediaCast(MediaRouterPlaybackActivity.this, router, route);
                }

                @Override
                public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "onRouteUnselected: route=" + route);

                    mMediaCast = null;

                }

                @Override
                public void onRoutePresentationDisplayChanged(
                        MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "onRoutePresentationDisplayChanged: route=" + route);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());

        // Create a route selector for the type of routes your app supports.
        mSelector = new MediaRouteSelector.Builder()
                // These are the framework-supported intents
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                //.addControlCategory(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
                .build();
                */
        mMediaRouteCast = new MediaRouteCast(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouteCast.startDiscovery();
    }

    @Override
    protected void onStop() {
        mMediaRouteCast.stopDiscovery();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu and configure the media router action provider.
        getMenuInflater().inflate(R.menu.media_router, menu);

        // Attach the MediaRouteSelector to the menu item
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(
                        mediaRouteMenuItem);
        //mediaRouteActionProvider.setRouteSelector(mSelector);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteCast.getRouteSelector());

        // Return true to show the menu.
        return true;
    }

}
