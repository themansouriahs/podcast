package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by apl on 22-08-2014.
 *
 * http://stackoverflow.com/questions/25178329/recyclerview-and-swiperefreshlayout
 */
public class FixedRecyclerView extends RecyclerView {

    private boolean mCanScrollRecyclerView = false;

    private boolean mRequestDisallowInterceptTouchEvent = false;

    public static final int DOWN = -1;
    private static final int UP = 1;

    public FixedRecyclerView(Context context) {
        super(context);
        init();
    }

    public FixedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FixedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        if (this.isInEditMode())
            return;
    }

    public static boolean mSeekbarSeeking = false;

    // http://stackoverflow.com/questions/25178329/recyclerview-and-swiperefreshlayout
    @Override
    public boolean canScrollVertically(int direction) {

        if (mSeekbarSeeking)
            return false;

        // check if scrolling up
        if (direction < 1) {
            boolean original = super.canScrollVertically(direction);
            mCanScrollRecyclerView = !original && getChildAt(0) != null && getChildAt(0).getTop() < 0 || original;
            Log.d("FixedRecyclerView", "(mCanScrollRecyclerView) canscroll: " + mCanScrollRecyclerView);
            return mCanScrollRecyclerView;
        }
        boolean canScroll = super.canScrollVertically(direction);
        Log.d("FixedRecyclerView", "(super) canscroll: " + canScroll);
        return canScroll;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d("FixedRecyclerView touch", "event" + ev.toString());
        //mCanScrollRecyclerView = canScrollVertically(DOWN);
        return super.onTouchEvent(ev);
        //return false;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean argDisallow) {
        mRequestDisallowInterceptTouchEvent = argDisallow;
        super.requestDisallowInterceptTouchEvent(argDisallow);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("FixedRecyclerView touch", "Intercept -> " + getCanScrollRecyclerView());
        if (mRequestDisallowInterceptTouchEvent)
            return false;

        Log.d("FixedRecyclerView", "Intercept -> " + getCanScrollRecyclerView());


        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_DOWN:
                return super.onInterceptTouchEvent(ev);
        }

        return getCanScrollRecyclerView();
        //return false;

        //return true;
        //return !scrolledToTop();
        //return true
        //return super.onInterceptTouchEvent(ev);
    }

    public boolean scrolledToTop() {
        return !canScrollVertically(DOWN);
    }

    public boolean getCanScrollRecyclerView() {
        return mCanScrollRecyclerView;
    }

    public void setCanScrollRecyclerView(boolean mCanScrollRecyclerView) {
        this.mCanScrollRecyclerView = mCanScrollRecyclerView;
    }

    /*
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mScrollable)
            return super.onTouchEvent(ev);

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Don't do anything with intercepted touch events if
        // we are not scrollable
        if (!mScrollable)
            return false;

        return super.onInterceptTouchEvent(ev);
    }
    */
}
