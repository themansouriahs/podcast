package org.bottiger.podcast;

import android.os.Bundle;
import android.view.Menu;

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
