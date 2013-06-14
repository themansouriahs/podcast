package org.bottiger.podcast.utils;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SettingsActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;

public class ThemeHelper {
	
	private Context mContext;
	private SharedPreferences prefs;
	
	public ThemeHelper(Context context) {
		mContext = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
	}
	
	// attr = R.attr.homeIcon
	public int getAttr(int attr) {
		
		TypedArray a = mContext.getTheme().obtainStyledAttributes(getTheme(prefs), new int[] {attr});     
		int attributeResourceId = a.getResourceId(0, 0);
		return attributeResourceId;
		//Drawable drawable = mContext.getResources().getDrawable(attributeResourceId);
		//return drawable;
	}
	
	public static int getTheme(SharedPreferences prefs) {
		if (prefs.getBoolean(SettingsActivity.DARK_THEME_KEY, false)) {
			return R.style.SoundWavesTheme;
		}
		
		return R.style.SoundWavesTheme_Light;
	}

}
