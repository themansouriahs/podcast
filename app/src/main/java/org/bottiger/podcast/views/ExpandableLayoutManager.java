package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by apl on 31-07-2014.
 */
public class ExpandableLayoutManager extends LinearLayoutManager {

    private boolean mcanScrollVertically = false;

    public ExpandableLayoutManager(Context context) {
        super(context);
        Log.d("ExpandableLayoutManager", "constrcutor");
    }

    @Override
    public void measureChild(View child, int widthUsed, int heightUsed) {
        super.measureChild(child, widthUsed, heightUsed);
    }

    @Override
    public void offsetChildrenHorizontal(int dx) {
        Log.d("ExpandableLayoutManager", "offsetx => " + dx);
        super.offsetChildrenHorizontal(dx);
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        Log.d("ExpandableLayoutManager", "offsety => " + dy);
        super.offsetChildrenVertical(dy);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        Log.d("ExpandableLayoutManager", "smoothScrollToPosition pos =>" + position);
        super.smoothScrollToPosition(recyclerView, state, position);
    }

    @Override
    public void startSmoothScroll(RecyclerView.SmoothScroller smoothScroller) {
        Log.d("ExpandableLayoutManager", "startSmoothScroll");
        super.startSmoothScroll(smoothScroller);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        Log.d("ExpandableLayoutManager", "scrollVerticallyBy, dy => "+ dy);
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    @Override
    public boolean canScrollVertically() {
        boolean canScroll = super.canScrollVertically();
        //Log.d("ExpandableLayoutManager", "canScrollV => "+ mcanScrollVertically);
        return mcanScrollVertically;
    }

    public void SetCanScrollVertically(boolean argCanScroll) {
        if (mcanScrollVertically != argCanScroll)
            Log.d("ExpandableLayoutManager", "set canScrollV => "+ mcanScrollVertically);

        mcanScrollVertically = argCanScroll;
    }

}
