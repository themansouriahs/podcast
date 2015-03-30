package org.bottiger.podcast.views.MultiShrink.MultiShrinkScroller;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * Created by apl on 27-03-2015.
 */
public abstract class AbstractMultiShrinkScroller extends FrameLayout {

    protected static final String TAG = "Scroller";

    protected float[] mLastEventPosition = { 0, 0 };
    protected int mTouchSlop;

    public AbstractMultiShrinkScroller(Context context) {
        super(context, null);
    }

    public AbstractMultiShrinkScroller(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public AbstractMultiShrinkScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AbstractMultiShrinkScroller(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    protected boolean motionShouldStartDrag(MotionEvent event) {
        final float deltaX = event.getX() - mLastEventPosition[0];
        final float deltaY = event.getY() - mLastEventPosition[1];

        Log.v(TAG, "updateLastEventPosition. DeltaY => " + deltaY + " RawY =>" + event.getRawY());
        final boolean draggedX = (deltaX > mTouchSlop || deltaX < -mTouchSlop);
        //final boolean draggedY = (deltaY > mTouchSlop || deltaY < -mTouchSlop);
        final boolean draggedY = Math.abs(deltaY) > mTouchSlop*2;
        //return draggedY && !draggedX;
        return draggedY;
    }
}
