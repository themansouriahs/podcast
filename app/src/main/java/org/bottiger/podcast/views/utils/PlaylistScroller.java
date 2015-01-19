package org.bottiger.podcast.views.utils;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Created by apl on 01-10-2014.
 */
public class PlaylistScroller extends Scroller {
    public PlaylistScroller(Context context) {
        super(context);
    }

    public PlaylistScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public PlaylistScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
    }
}
