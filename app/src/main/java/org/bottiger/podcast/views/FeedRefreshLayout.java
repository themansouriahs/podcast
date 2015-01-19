package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import org.bottiger.podcast.R;

/**
 * Created by apl on 20-11-2014.
 */
public class FeedRefreshLayout extends SwipeRefreshLayout {

    private RecyclerView mRecyclerView;

    public FeedRefreshLayout(Context context) {
        super(context);
        init(context);
    }

    public FeedRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context argContext) {
        float offset = argContext.getResources().getDimension(R.dimen.header_bar_height);
        setProgressViewOffset (false, 0, (int)(offset*1.2));
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }
}
