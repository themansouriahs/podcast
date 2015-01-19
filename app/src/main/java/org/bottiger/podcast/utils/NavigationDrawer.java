package org.bottiger.podcast.utils;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class NavigationDrawer {
	
	private MainActivity mActivity;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;
	
	public NavigationDrawer(MainActivity activity) {
		this.mActivity = activity;
	}
	
	public void initialize() {    }


}
