package org.bottiger.podcast;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Window;
import android.view.WindowManager;

import com.squareup.otto.Bus;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.PlayerService;

public class TopActivity extends ActionBarActivity {
	
	private static SharedPreferences prefs;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        super.onCreate(savedInstanceState);
        SoundWaves.getBus().register(this);

        boolean transparentStatus = transparentNavigationBar();
        /*
        if (transparentStatus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }*/

        if (transparentStatus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition transition = new Slide();
            getWindow().setEnterTransition(transition);
            getWindow().setExitTransition(transition);
        }

        if (ApplicationConfiguration.DEBUGGING)
            ViewServer.get(this).addWindow(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

    @Override
    protected void onDestroy() {
        SoundWaves.getBus().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SoundWaves.sAnalytics.activityPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SoundWaves.sAnalytics.activityResume();
    }

    public static SharedPreferences getPreferences() {
		return prefs;
	}

    /**
     * Override this if the navigation bar should remain opaque
     */
    protected boolean transparentNavigationBar() {
        return true;
    }

    @Nullable
    protected Playlist getPlaylist() {
        PlayerService ps = MainActivity.sBoundPlayerService;

        if (ps != null)
            return ps.getPlaylist();

        return null;
    }

}
