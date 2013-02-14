package info.bottiger.podcast.utils;

import info.bottiger.podcast.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

public class ThemeHelper {
	
	private Context mContext;
	
	public ThemeHelper(Context context) {
		mContext = context;
	}
	
	// attr = R.attr.homeIcon
	public int getAttr(int attr) {
		TypedArray a = mContext.getTheme().obtainStyledAttributes(R.style.SoundWavesTheme_Light, new int[] {attr});     
		int attributeResourceId = a.getResourceId(0, 0);
		return attributeResourceId;
		//Drawable drawable = mContext.getResources().getDrawable(attributeResourceId);
		//return drawable;
	}

}
