package org.bottiger.podcast.views;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.bottiger.podcast.R;

/**
 * Created by Arvid on 9/7/2015.
 */
public class TopPlayerBehavior extends CoordinatorLayout.Behavior<TopPlayer> {

    private final String TAG = getClass().getName();

    public TopPlayerBehavior() {
        Log.d(TAG, "Created");
    }

    public TopPlayerBehavior(Context context, AttributeSet attrs) {
        Log.d(TAG, "Created");
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, TopPlayer child, View dependency) {
        boolean val = (dependency.getId() == R.id.session_photo_container);
        return val;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, TopPlayer child, View dependency) {
        float translationY = dependency.getScrollY();

        //Utils.logD(this.getClass().getSimpleName(), "dependency changed by" + translationY);
        return true;
    }

}
