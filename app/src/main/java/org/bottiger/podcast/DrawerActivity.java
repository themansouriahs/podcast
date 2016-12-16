package org.bottiger.podcast;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import org.bottiger.podcast.playlist.PlaylistData;
import org.bottiger.podcast.utils.TransitionUtils;
import org.bottiger.podcast.views.SlidingTab.SlidingTabLayout;

import java.util.LinkedList;
import java.util.List;

public abstract class DrawerActivity extends MediaRouterPlaybackActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String FEEDBACK = "[Feedback]"; // NoI18N

    protected SharedPreferences mSharedPreferences;

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;

    private NavigationView mNavigationView;
    private NavigationView mNavigationViewBottom;
    private Toolbar mToolbar;

    public List<TopFound> listeners = new LinkedList<>();
    public interface TopFound {
        void topfound(int i);
    }


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_drawer);
        mNavigationViewBottom = (NavigationView) findViewById(R.id.navigation_drawer_bottom);
        mToolbar = (Toolbar) findViewById(R.id.soundwaves_toolbar);

        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationViewBottom.setNavigationItemSelectedListener(this);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar, //R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();



    }


    // More details: http://blog.xebia.com/2015/06/09/android-design-support-navigationview/
    public boolean onNavigationItemSelected (MenuItem item) {

        boolean isHandled = false;

        switch (item.getItemId()) {
            case R.id.navigation_clear: {
                isHandled = true;
                PlaylistData pld = new PlaylistData();
                pld.reset = true;
                SoundWaves.getRxBus().send(pld);
                break;
            }
            case R.id.navigation_feedback: {
                isHandled = true;
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", ApplicationConfiguration.ACRA_MAIL, null)); // NoI18N
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_mail_client_picker)));
                break;
            }
            case R.id.navigation_downloading: {
                isHandled = true;
                TransitionUtils.openDownloadManager(this);
                break;
            }
            case R.id.navigation_settings: {
                isHandled = true;
                TransitionUtils.openSettings(this);
                break;
            }
        }

        if (isHandled) {
            mDrawerLayout.closeDrawers();
        }

        return isHandled;
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