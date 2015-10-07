package org.bottiger.podcast;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.TransitionUtils;
import org.bottiger.podcast.views.dialogs.DialogAddPodcast;

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
    private Menu mMenu;
    private OttoSubscriberHack mOttoSubscriberHack = new OttoSubscriberHack();

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        super.onCreate(savedInstanceState);

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
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        SoundWaves.getBus().unregister(this);
        SoundWaves.getBus().unregister(mOttoSubscriberHack);
        super.onPause();
        SoundWaves.sAnalytics.activityPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SoundWaves.getBus().register(this);
        SoundWaves.getBus().register(mOttoSubscriberHack);
        SoundWaves.sAnalytics.activityResume();
        initDownloadManagerOptionsMenu();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.top_options_menu, menu);
        initDownloadManagerOptionsMenu();
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Right corner menu options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_download_manager: {
                TransitionUtils.openDownloadManager(this);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {

        if (PermissionDenied(grantResults)) {
            return;
        }

        switch (requestCode) {
            case PERMISSION_TO_DOWNLOAD: {
                PlayerService ps = SoundWaves.sBoundPlayerService;
                if (ps == null)
                    return;
                ps.getDownloadManager().startDownload();
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

    private void initDownloadManagerOptionsMenu() {
        if (mMenu == null)
            return;

        MenuItem item = mMenu.findItem(R.id.menu_download_manager);
        PlayerService ps = PlayerService.getInstance();

        if (ps == null)
            return;

        item.setVisible(ps.getDownloadManager().getDownloadingItem() != null);
    }

    // https://github.com/square/otto/issues/83
    class OttoSubscriberHack {

        @Subscribe
        public void showDownloadManager(SoundWavesDownloadManager.DownloadManagerChanged event)
        {
            MenuItem item = TopActivity.this.mMenu.findItem(R.id.menu_download_manager);
            item.setVisible(event.queueSize > 0);
        }

    }

}
