package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.widget.ScrollerCompat;
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

    private ScrollerCompat mScroller;
    private int mLastY = 0;


    public static final int DOWN = -1;
    private static final int UP = 1;

    public FixedRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public FixedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FixedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mScroller.computeScrollOffset()) {
            Log.d("FixedRecyclerView", "(fling) Y: " + mScroller.getCurrY());
            scrollBy(0, mScroller.getCurrY()- mLastY);
            mLastY = mScroller.getCurrY();
            postInvalidateDelayed(16);
        }
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        LayoutManager lm = getLayoutManager();
        mLastY = 0;
        mScroller.fling(0, 0, velocityX, velocityY, 0, 0, -2000 ,4000);
        return true;
    }

    protected void init(Context context) {
        mScroller = ScrollerCompat.create(context);
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
        super.requestDisallowInterceptTouchEvent(argDisallow);
    }
}
