package org.bottiger.podcast;

import java.util.ArrayList;
import java.util.Arrays;

import org.bottiger.podcast.adapters.PlaylistContentSpinnerAdapter;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.PlaylistData;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.navdrawer.NavigationDrawerMenuGenerator;
import org.bottiger.podcast.views.MultiSpinner;
import org.bottiger.podcast.views.dialogs.DialogPlaylistContent;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

public abstract class DrawerActivity extends MediaRouterPlaybackActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String FEEDBACK = "[Feedback]"; // NoI18N

    protected SharedPreferences mSharedPreferences;

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;
    private Activity mActivity;

    protected RelativeLayout mDrawerMainContent;
    private NavigationView mNavigationView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mDrawerMainContent = (RelativeLayout) findViewById(R.id.outer_container);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_drawer);
        mNavigationView.setNavigationItemSelectedListener(this);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar, //R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        // if we can use windowTranslucentNavigation=true
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //FrameLayout.MarginLayoutParams params = (FrameLayout.MarginLayoutParams) mDrawerTable.getLayoutParams();
            //params.topMargin = getStatusBarHeight(getResources());

            RelativeLayout.MarginLayoutParams params2 = (RelativeLayout.MarginLayoutParams) mDrawerMainContent.getLayoutParams();
            params2.topMargin = getStatusBarHeight(getResources());

            mDrawerMainContent.setLayoutParams(params2);
            //mDrawerTable.setLayoutParams(params);
        }

    }


    // More details: http://blog.xebia.com/2015/06/09/android-design-support-navigationview/
    public boolean onNavigationItemSelected (MenuItem item) {

        switch (item.getItemId()) {
            case R.id.navigation_clear: {
                PlaylistData pld = new PlaylistData();
                pld.reset = true;
                SoundWaves.getBus().post(pld);
                return true;
            }
            case R.id.navigation_feedback: {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", ApplicationConfiguration.ACRA_MAIL, null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_mail_client_picker)));
                return true;
            }

        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * MenuInflater inflater = getMenuInflater();
		 * inflater.inflate(R.menu.main, menu);
		 */
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
	}

}