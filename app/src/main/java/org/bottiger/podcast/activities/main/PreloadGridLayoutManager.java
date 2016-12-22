package org.bottiger.podcast.activities.main;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by aplb on 22-12-2016.
 */

public class PreloadGridLayoutManager extends GridLayoutManager {
    public PreloadGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PreloadGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public PreloadGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    protected int getExtraLayoutSpace (RecyclerView.State state) {
        return 10000;
    }

}
