package org.bottiger.podcast.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by aplb on 27-01-2016.
 */
public class TopPlayerScroller extends PlaylistBehavior {

    private final String TAG = "TopPlayerScroller";

    public TopPlayerScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.v(TAG, "Created");
    }

}