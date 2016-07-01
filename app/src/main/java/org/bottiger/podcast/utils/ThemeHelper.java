package org.bottiger.podcast.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;

import org.bottiger.podcast.R;

public class ThemeHelper {
	
	private Context mContext;
	private SharedPreferences prefs;
	
	public ThemeHelper(Context context) {
		mContext = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
	}
	
	// attr = R.attr.homeIcon
	public int getAttr(int attr) {
		
		TypedArray a = mContext.getTheme().obtainStyledAttributes(R.style.SoundWavesTheme_Light, new int[] {attr});
		int attributeResourceId = a.getResourceId(0, 0);
		a.recycle();
		return attributeResourceId;
	}

}
