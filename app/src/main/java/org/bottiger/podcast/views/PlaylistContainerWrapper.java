package org.bottiger.podcast.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.UIUtils;

/**
 * Created by aplb on 28-01-2016.
 */
public class PlaylistContainerWrapper extends CoordinatorLayout implements NestedScrollingParent, NestedScrollingChild, GestureDetector.OnGestureListener {

    private static final String TAG = PlaylistContainerWrapper.class.getSimpleName();

    @Nullable private TopPlayer mTopPlayer;
    @Nullable private RecyclerView mRecyclerView;

    private static final int DOWN = -1;
    private static final int UP = 1;

    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private GestureDetectorCompat mDetector;
    private int mScrollPointerId;
    private int mInitialTouchX;
    private int mLastTouchX;
    private int mInitialTouchY;
    private int mLastTouchY;

    public PlaylistContainerWrapper(Context context) {
        this(context, null);
    }

    public PlaylistContainerWrapper(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public PlaylistContainerWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new GestureDetectorCompat(context, this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
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

    /**
     * The RecyclerView is not currently scrolling.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The RecyclerView is currently being dragged by outside input such as user touch input.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The RecyclerView is currently animating to a final position while not under
     * outside control.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_SETTLING = 2;
    private int mScrollState = SCROLL_STATE_IDLE;
    private VelocityTracker mVelocityTracker;
    private boolean mIgnoreMotionEventTillDown;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private int mTouchSlop;
    private final ViewFlinger mViewFlinger = new ViewFlinger();
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {


        final boolean canScrollHorizontally = false; //mLayout.canScrollHorizontally();
        final boolean canScrollVertically = true; // mLayout.canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);

        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mIgnoreMotionEventTillDown) {
                    mIgnoreMotionEventTillDown = false;
                }
                mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                if (mScrollState == SCROLL_STATE_SETTLING) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                }

                // Clear the nested offsets
                mNestedOffsets[0] = mNestedOffsets[1] = 0;

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis);
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN:
                mScrollPointerId = MotionEventCompat.getPointerId(e, actionIndex);
                mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
                final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final int dx = x - mInitialTouchX;
                    final int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = mInitialTouchX + mTouchSlop * (dx < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = mInitialTouchY + mTouchSlop * (dy < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
            } break;

            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
            } break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.clear();
                stopNestedScroll();
            } break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            }
        }
        return mScrollState == SCROLL_STATE_DRAGGING;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        /*
        if (mLayoutFrozen || mIgnoreMotionEventTillDown) {
            return false;
        }
        if (dispatchOnItemTouch(e)) {
            cancelTouch();
            return true;
        }

        if (mLayout == null) {
            return false;
        }
        */

        final boolean canScrollHorizontally = false; //mLayout.canScrollHorizontally();
        final boolean canScrollVertically = true; // mLayout.canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;

        final MotionEvent vtev = MotionEvent.obtain(e);
        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis);
            } break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(e, actionIndex);
                mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
            } break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
                final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        if (dx > 0) {
                            dx -= mTouchSlop;
                        } else {
                            dx += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        if (dy > 0) {
                            dy -= mTouchSlop;
                        } else {
                            dy += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];

                    if (scrollByInternal(
                            canScrollHorizontally ? dx : 0,
                            canScrollVertically ? dy : 0,
                            vtev)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
            } break;

            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
            } break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                final float xvel = canScrollHorizontally ?
                        -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                final float yvel = canScrollVertically ?
                        -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
                if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetTouch();
            } break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            } break;
        }

        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();

        return true;
    }

    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        if (BuildConfig.DEBUG && false) {
            Log.d(TAG, "setting scroll state to " + state + " from " + mScrollState,
                    new Exception());
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            stopScrollersInternal();
        }
        //dispatchOnScrollStateChanged(state);
    }

    /**
     * Similar to {@link #stopScroll()} but does not set the state.
     */
    private void stopScrollersInternal() {
        mViewFlinger.stop();
        /*
        if (mLayout != null) {
            mLayout.stopSmoothScroller();
        }
        */
    }

    private void cancelTouch() {
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
    }

    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        stopNestedScroll();
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (MotionEventCompat.getPointerId(e, actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = MotionEventCompat.getPointerId(e, newIndex);
            mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, newIndex) + 0.5f);
        }
    }


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

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, null);
        super.onNestedPreScroll(target, dx, dy, consumed);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {

        Log.w(TAG, "touchdebug nestedscroll: " + target.toString());
        scrollInternal(dyUnconsumed, dyConsumed, new int[2], true);
    }

    /*
    @Override
    public boolean onTouchEvent(MotionEvent event){
        Log.w(TAG, "touchdebug touch : " + event.getAction());
        final boolean handled = mDetector.onTouchEvent(event);
        if (!handled && event.getAction() == MotionEvent.ACTION_UP) {
            stopNestedScroll();
        }
        //return false;
        return handled;
    }
    */


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

    private boolean scrollByInternal(int x, int y, MotionEvent ev) {
        return onScroll(ev ,ev, x ,y);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        scrollInternal(distanceY, 0, new int[2], true);
        return true;
    }

    private void scrollInternal(float distanceY, float distanceYconsumed, int[] offsetInWindow, boolean argNotifyAboutScrolling) {
        Log.v(TAG, "scrollInternal called");


        if (mTopPlayer == null) {
            mTopPlayer = (TopPlayer) findViewById(R.id.top_player);
        }

        if (mRecyclerView == null) {
            mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        }

        int[] consumed = new int[2];
        consumed[1] = (int)distanceYconsumed;

        if (argNotifyAboutScrolling)
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        //int[] offsetInWindow = new int[2];
        if (argNotifyAboutScrolling)
            dispatchNestedPreScroll(0, (int) distanceY, consumed, offsetInWindow);

        int consumedY = consumed[1];
        int unconsumedY = (int)(distanceY); //int unconsumedY = (int)(distanceY-consumedY-offsetInWindow[1]);

        if (argNotifyAboutScrolling)
            dispatchNestedScroll(0, consumedY, 0, unconsumedY, offsetInWindow);

        if (mTopPlayer == null || mRecyclerView == null || mTopPlayer.isFullscreen()) {
            return;
        }

        if (distanceY < 0) {
            if (mRecyclerView.canScrollVertically(DOWN)) {
                // it cannot be larger, scroll recyclerview
                Log.v(TAG, "scroll.run, mRecyclerView1");
                mRecyclerView.scrollBy(0, (int) distanceY);
            } else {
                Log.v(TAG, "scroll.run, mTopPlayer2");
                //mTopPlayer.scrollExternal(distanceY, false);
                mTopPlayer.scrollBy(distanceY);
            }
        } else {
            if (mTopPlayer.isMinimumSize()) {
                Log.v(TAG, "scroll.run, mRecyclerView2");
                mRecyclerView.scrollBy(0, (int) distanceY);
            } else {
                int canscrollup = canScrollUp(mRecyclerView);
                if (canscrollup > 0) {
                    Log.v(TAG, "scroll.run, mTopPlayer1 diffY: " + distanceY + " canscroll: " + canscrollup);

                    distanceY = distanceY < canscrollup ? distanceY : canscrollup;
                    //mTopPlayer.scrollExternal(distanceY, false);
                    mTopPlayer.scrollBy(distanceY);
                }
            }
        }
    }

    private boolean canScrollDown() {
        if (mTopPlayer == null)
            return false;

        return !mTopPlayer.isMaximumSize();
    }

    private static int canScrollUp(RecyclerView argRecyclerView) {
        int lastItemPos = ((LinearLayoutManager)argRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();

        View v = (argRecyclerView.getLayoutManager()).findViewByPosition(lastItemPos);

        if (v == null)
            return -1;

        int[] loc = new int[2];
        v.getLocationOnScreen(loc);

        Context context = argRecyclerView.getContext();

        int screen = UIUtils.getScreenHeight(context);
        int navBar = UIUtils.NavigationBarHeight(context);

        int realloc = (int) (loc[1] + argRecyclerView.getTranslationY());
        int realloc2 = loc[1] + v.getHeight();

        return (realloc2+navBar)-screen; //
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second.
     * If the velocity given is below the system-defined minimum this method will return false
     * and no fling will occur.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     * @return true if the fling was started, false if the velocity was too low to fling or
     * LayoutManager does not support scrolling in the axis fling is issued.
     *
     * @see LayoutManager#canScrollVertically()
     * @see LayoutManager#canScrollHorizontally()
     */
    public boolean fling(int velocityX, int velocityY) {
        final boolean canScrollHorizontal = false;
        final boolean canScrollVertical = true;

        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            // If we don't have any velocity, return false
            return false;
        }

        if (!dispatchNestedPreFling(velocityX, velocityY)) {
            final boolean canScroll = canScrollHorizontal || canScrollVertical;
            dispatchNestedFling(velocityX, velocityY, canScroll);

            if (canScroll) {
                velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
                velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
                mViewFlinger.fling(velocityX, velocityY);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return true;
    }

    void dispatchOnScrolled(int hresult, int vresult) {
        // Pass the current scrollX/scrollY values; no actual change in these properties occurred
        // but some general-purpose code may choose to respond to changes this way.
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        onScrollChanged(scrollX, scrollY, scrollX, scrollY);
    }

    private class ViewFlinger implements Runnable {
        private int mLastFlingX;
        private int mLastFlingY;
        private ScrollerCompat mScroller;
        private Interpolator mInterpolator = sQuinticInterpolator;

        private static final int MAX_SCROLL_DURATION = 2000;


        // When set to true, postOnAnimation callbacks are delayed until the run method completes
        private boolean mEatRunOnAnimationRequest = false;

        // Tracks if postAnimationCallback should be re-attached when it is done
        private boolean mReSchedulePostAnimationCallback = false;

        public ViewFlinger() {
            mScroller = ScrollerCompat.create(getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            disableRunOnAnimationRequests();
            //consumePendingUpdateOperations();
            // keep a local reference so that if it is changed during onAnimation method, it won't
            // cause unexpected behaviors
            final ScrollerCompat scroller = mScroller;
            if (scroller.computeScrollOffset()) {

                final int x = scroller.getCurrX();
                final int y = scroller.getCurrY();
                final int dx = x - mLastFlingX;
                final int dy = y - mLastFlingY;
                int hresult = 0;
                int vresult = 0;
                mLastFlingX = x;
                mLastFlingY = y;
                int overscrollX = 0;
                int overscrollY = 0;

                Log.v(TAG, "scrolling scrolling scrolling: " + dy + " y: " + y + " lastY: " + mLastFlingY + " time:");
                PlaylistContainerWrapper.this.scrollInternal(dy, 0 ,null, false);
                vresult = dy;

                if (!PlaylistContainerWrapper.this.canScrollDown()) {
                    vresult = 0;
                    scroller.abortAnimation();
                }

                if (overscrollX != 0 || overscrollY != 0) {
                    final int vel = (int) scroller.getCurrVelocity();

                    int velX = 0;
                    if (overscrollX != x) {
                        velX = overscrollX < 0 ? -vel : overscrollX > 0 ? vel : 0;
                    }

                    int velY = 0;
                    if (overscrollY != y) {
                        velY = overscrollY < 0 ? -vel : overscrollY > 0 ? vel : 0;
                    }

                    if ((velX != 0 || overscrollX == x || scroller.getFinalX() == 0) &&
                            (velY != 0 || overscrollY == y || scroller.getFinalY() == 0)) {
                        scroller.abortAnimation();
                    }


                }
                if (hresult != 0 || vresult != 0) {
                    dispatchOnScrolled(hresult, vresult);
                }

                if (!awakenScrollBars()) {
                    invalidate();
                }

                final boolean fullyConsumedVertical = dy != 0 && true
                        && vresult == dy;
                final boolean fullyConsumedHorizontal = dx != 0 && false
                        && hresult == dx;
                final boolean fullyConsumedAny = (dx == 0 && dy == 0) || fullyConsumedHorizontal
                        || fullyConsumedVertical;

                if (scroller.isFinished() || !fullyConsumedAny) {
                    setScrollState(SCROLL_STATE_IDLE); // setting state to idle will stop this.
                } else {
                    postOnAnimation();
                }
            }
            // call this after the onAnimation is complete not to have inconsistent callbacks etc.
            /*
            if (smoothScroller != null) {
                if (smoothScroller.isPendingInitialRun()) {
                    smoothScroller.onAnimation(0, 0);
                }
                if (!mReSchedulePostAnimationCallback) {
                    smoothScroller.stop(); //stop if it does not trigger any scroll
                }
            }
            */
            enableRunOnAnimationRequests();
        }

        private void disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false;
            mEatRunOnAnimationRequest = true;
        }

        private void enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false;
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation();
            }
        }

        void postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true;
            } else {
                removeCallbacks(this);
                ViewCompat.postOnAnimation(PlaylistContainerWrapper.this, this);
            }
        }

        public void fling(int velocityX, int velocityY) {
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.fling(0, 0, velocityX, velocityY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }

        public void smoothScrollBy(int dx, int dy) {
            smoothScrollBy(dx, dy, 0, 0);
        }

        public void smoothScrollBy(int dx, int dy, int vx, int vy) {
            smoothScrollBy(dx, dy, computeScrollDuration(dx, dy, vx, vy));
        }

        private float distanceInfluenceForSnapDuration(float f) {
            f -= 0.5f; // center the values about 0.
            f *= 0.3f * Math.PI / 2.0f;
            return (float) Math.sin(f);
        }

        private int computeScrollDuration(int dx, int dy, int vx, int vy) {
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            final boolean horizontal = absDx > absDy;
            final int velocity = (int) Math.sqrt(vx * vx + vy * vy);
            final int delta = (int) Math.sqrt(dx * dx + dy * dy);
            final int containerSize = horizontal ? getWidth() : getHeight();
            final int halfContainerSize = containerSize / 2;
            final float distanceRatio = Math.min(1.f, 1.f * delta / containerSize);
            final float distance = halfContainerSize + halfContainerSize *
                    distanceInfluenceForSnapDuration(distanceRatio);

            final int duration;
            if (velocity > 0) {
                duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
            } else {
                float absDelta = (float) (horizontal ? absDx : absDy);
                duration = (int) (((absDelta / containerSize) + 1) * 300);
            }
            return Math.min(duration, MAX_SCROLL_DURATION);
        }

        public void smoothScrollBy(int dx, int dy, int duration) {
            smoothScrollBy(dx, dy, duration, sQuinticInterpolator);
        }

        public void smoothScrollBy(int dx, int dy, int duration, Interpolator interpolator) {
            if (mInterpolator != interpolator) {
                mInterpolator = interpolator;
                mScroller = ScrollerCompat.create(getContext(), interpolator);
            }
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.startScroll(0, 0, dx, dy, duration);
            postOnAnimation();
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }

    }

    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

}
