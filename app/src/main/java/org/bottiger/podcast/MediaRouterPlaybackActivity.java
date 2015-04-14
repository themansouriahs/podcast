package org.bottiger.podcast;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.bottiger.podcast.flavors.MediaCast.VendorMediaRouteCast;

/**
 * Created by apl on 28-03-2015.
 */
public class MediaRouterPlaybackActivity extends ToolbarActivity {

    private static final String TAG = "MediaRouterPlayback";

    protected VendorMediaRouteCast mMediaRouteCast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaRouteCast = new VendorMediaRouteCast(this);
        mMediaRouteCast.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        mMediaRouteCast.stopDiscovery();
        super.onStop();
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
            //mediaRouteActionProvider.setRouteSelector(mSelector);
            mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
        }

        // Return true to show the menu.
        return true;
    }

}
