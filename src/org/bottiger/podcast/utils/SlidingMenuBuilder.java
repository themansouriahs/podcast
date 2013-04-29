package org.bottiger.podcast.utils;

import org.bottiger.podcast.MainActivity.SectionsPagerAdapter;
import org.bottiger.podcast.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Switch;

import com.slidingmenu.lib.SlidingMenu;

public class SlidingMenuBuilder {
	
	final public static String episodesToShowKey = "num_of_episodes";
	
	final public static String showListenedKey = "show_listened";
	final public static Boolean showListenedVal = false;
	
	private static Context mContext;
	private static SharedPreferences sharedPreferences;
	
	public static void build(Activity activity, final SectionsPagerAdapter sectionsPagerAdapter) {
		mContext = activity.getApplicationContext();
		
		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		
		// Initialize the sliding menu
		
		SlidingMenu menu = new SlidingMenu(activity);

		menu.setMode(SlidingMenu.LEFT);
		menu.setShadowWidthRes(R.dimen.shadow_width);
		// menu.setShadowDrawable(R.drawable.shadow);
		menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		menu.setFadeDegree(0.35f);
		menu.attachToActivity(activity, SlidingMenu.SLIDING_CONTENT);
		menu.setTouchModeAbove(SlidingMenu.LEFT);
		menu.setMenu(R.layout.slidemenu_filter);
		//setSlidingActionBarEnabled(true);
		
		Switch showListenedSwitch = (Switch) activity.findViewById(R.id.slidebar_show_listened);
		showListenedSwitch.setChecked(sharedPreferences.getBoolean(showListenedKey, showListenedVal));
		
		showListenedSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    	sharedPreferences.edit().putBoolean(showListenedKey, isChecked).apply();
		    	sectionsPagerAdapter.notifyDataSetChanged();
		    }
		});
		
		final Spinner numberOfEpisodes = (Spinner) activity.findViewById(R.id.slidebar_number_of_episodes);
		
		numberOfEpisodes.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				Integer number = Integer.valueOf(numberOfEpisodes.getSelectedItem().toString());
				sharedPreferences.edit().putInt("episodes_to_show", number);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}	
		});
		
		//activity.mSectionsPagerAdapter;
	}

}
