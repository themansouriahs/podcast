package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by apl on 15-11-2014.
 */
public class FeedRecyclerView extends RecyclerView {
    public FeedRecyclerView(Context context) {
        super(context);
    }

    public FeedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FeedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        // check if scrolling up
        if (direction < 1) {
            boolean original = super.canScrollVertically(direction);
            return !original && getChildAt(0) != null && getChildAt(0).getTop() < 0 || original;
        }
        return super.canScrollVertically(direction);

    }

    /*
    @Override
    public boolean getCanScrollRecyclerView() {
        return true;
    }
    */
}
