package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by aplb on 18-09-2015.
 */
public class CustomLinearLayoutManager extends LinearLayoutManager {
    public CustomLinearLayoutManager(Context context) {
        super(context);
    }

    public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public CustomLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    /**
     * This is a hack to prevent an IndexOutOfBound exception
     *
     * https://code.google.com/p/android/issues/detail?id=77846
     * https://code.google.com/p/android/issues/detail?can=2&start=0&num=100&q=&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&groupby=&sort=&id=77232
     *
     * @param arg0
     * @param arg1
     */
    public void onLayoutChildren(RecyclerView.Recycler arg0, RecyclerView.State arg1) {
        try {
            super.onLayoutChildren(arg0, arg1);
        } catch (IndexOutOfBoundsException e) {
            Log.e("RecyclerFail", "onLayoutChildren :" + e.toString());
            removeViewAt(0);
        } catch (IllegalStateException ise) {
            Log.e("RecyclerFail", "onLayoutChildren :" + ise.toString());
        }
    }

}
