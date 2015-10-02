package org.bottiger.podcast;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.OPMLImportExport;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TopActivity extends AppCompatActivity {

    // Filesystem Permisssion
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PERMISSION_TO_DOWNLOAD, PERMISSION_TO_IMPORT_EXPORT})
    public @interface PermissionCallback {}
    public static final int PERMISSION_TO_DOWNLOAD = 1;
    public static final int PERMISSION_TO_IMPORT_EXPORT = 2;
	
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
        PlayerService ps = SoundWaves.sBoundPlayerService;

        if (ps != null)
            return ps.getPlaylist();

        return null;
    }

    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {

        if (PermissionDenied(grantResults)) {
            return;
        }

        switch (requestCode) {
        //if (requestCode == PERMISSION_TO_DOWNLOAD) {
            case PERMISSION_TO_DOWNLOAD: {
                EpisodeDownloadManager.startDownload(this);
                return;
            }
            case PERMISSION_TO_IMPORT_EXPORT: {
                SubscriptionsFragment.openImportExportDialog(this);
                return;
            }
        }
    }

    protected void importOPMLButtonCallback() {
        SubscriptionsFragment.openImportExportDialog(this);
    }

    /**
     *
     * @param grantResults
     * @return False if one or more permission was denied
     */
    private boolean PermissionDenied(int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                return true;
        }

        return false;
    }

}
