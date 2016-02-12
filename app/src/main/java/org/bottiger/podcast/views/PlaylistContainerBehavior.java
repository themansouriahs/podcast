package org.bottiger.podcast.views;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import org.bottiger.podcast.R;

import java.util.List;

/**
 * Created by aplb on 28-01-2016.
 */
public class PlaylistContainerBehavior extends CoordinatorLayout implements NestedScrollingParent, NestedScrollingChild, GestureDetector.OnGestureListener {

    private static final String TAG = "PlaylistContainerBe";

    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private GestureDetectorCompat mDetector;
    private int mScrollPointerId;
    private int mInitialTouchX;
    private int mLastTouchX;
    private int mInitialTouchY;
    private int mLastTouchY;

    public PlaylistContainerBehavior(Context context) {
        this(context, null);
    }

    public PlaylistContainerBehavior(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public PlaylistContainerBehavior(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new GestureDetectorCompat(context, this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(true);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mNestedScrollingChildHelper.onDetachedFromWindow();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        Log.w(TAG, "touchdebug intercep: " + e.getAction());
        /*
        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        boolean doIntercept = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:

                mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                doIntercept = true;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
                if (index < 0) {
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                doIntercept = false;
            }
        }
        return doIntercept;
        */
        return false;
    }

    /*
    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child,
                                  View target, int dx, int dy, int[] consumed) {
        if (dy != 0 && !mSkipNestedPreScroll) {
            int min, max;
            if (dy < 0) {
                // We're scrolling down
                min = -child.getTotalScrollRange();
                max = min + child.getDownNestedPreScrollRange();
            } else {
                // We're scrolling up
                min = -child.getUpNestedPreScrollRange();
                max = 0;
            }
            consumed[1] = super.scroll(coordinatorLayout, child, dy, min, max);
        }
    }
    */

    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        ViewParent p = this.getParent().getParent();

        if (p instanceof CoordinatorLayout) {
            CoordinatorLayout cl = ((CoordinatorLayout)p);
            AppBarLayout apl = (AppBarLayout) cl.findViewById(R.id.app_bar_layout);
            CoordinatorLayout.LayoutParams lp = (LayoutParams) apl.getLayoutParams();
            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onNestedFling(this, apl, target, velocityX, velocityY,
                        consumed);
            }
        }

        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    /*
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {

        Log.w(TAG, "touchdebug nestedscroll: " + target.toString());
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        scrollInternal(dyUnconsumed);
        stopNestedScroll();
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }
    */

    @Override
    public boolean onTouchEvent(MotionEvent event){
        Log.w(TAG, "touchdebug touch : " + event.getAction());
        final boolean handled = mDetector.onTouchEvent(event);
        if (!handled && event.getAction() == MotionEvent.ACTION_UP) {
            stopNestedScroll();
        }
        return false;
        //return handled;
    }


    @Override
    public boolean onDown(MotionEvent e) {
        //startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        scrollInternal(distanceY);
        return true;
    }

    private void scrollInternal(float distanceY) {
        dispatchNestedPreScroll(0, (int) distanceY, null, null);
        dispatchNestedScroll(0, 0, 0, 0, null);
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return true;
    }
}
