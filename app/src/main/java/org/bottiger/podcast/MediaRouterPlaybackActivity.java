package org.bottiger.podcast;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.bottiger.podcast.flavors.MediaCast.VendorMediaRouteCast;
import org.bottiger.podcast.player.googlecast.GoogleCastPlayer;
import org.bottiger.podcast.service.PlayerService;

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
        mMediaRouteCast.onCreate();
    }

    @Override
    protected void onResume() {
        mMediaRouteCast.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMediaRouteCast.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMediaRouteCast.setupMediaButton(this, menu, R.id.media_route_menu_item);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        SoundWaves.getAppContext(this).setPlayer(mMediaRouteCast);
    }

}
