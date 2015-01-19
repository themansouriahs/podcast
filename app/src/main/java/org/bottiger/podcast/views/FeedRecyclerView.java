package org.bottiger.podcast.views;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by apl on 15-11-2014.
 */
public class FeedRecyclerView extends FixedRecyclerView {
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
    public boolean getCanScrollRecyclerView() {
        return true;
    }
}
