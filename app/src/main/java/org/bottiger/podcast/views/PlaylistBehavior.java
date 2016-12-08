package org.bottiger.podcast.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.UIUtils;

/**
 * Created by aplb on 08-09-2015.
 */
public class PlaylistBehavior extends CoordinatorLayout.Behavior<View> {

    private final String TAG = "PlaylistBehavior";
    private static final boolean DEBUG = false;

    @Nullable private TopPlayer mTopPlayer;
    @Nullable private RecyclerView mRecyclerView;
    private PlaylistContainerWrapper mPlaylistContainerWrapper;

    private int mRecyclerViewBottomPadding;

    public static final int MAX_VELOCITY_Y = 5000;

    private static final int DOWN = -1;
    private static final int UP = 1;

    // From AppBarLayout
    private Runnable mFlingRunnable;
    private ScrollerCompat mScroller;

    public PlaylistBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.v(TAG, "Created");
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
        return super.onInterceptTouchEvent(parent, child, ev);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        if (DEBUG)
            Log.v(TAG, "layoutDependsOn, child: " + child.getClass().getName() + " dependency: " + dependency.getClass().getName());
        boolean corectChild = (child.getId() == R.id.my_recycler_view);
        boolean correctDependency = (dependency.getId() == R.id.top_player);
        return corectChild && correctDependency && super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (DEBUG) {
            Log.v(TAG, "onDependentViewChanged, child: " + child.getClass().getName() + " dependency: " + dependency.getClass().getName());
        }

        if (mTopPlayer != null && mRecyclerView != null) {
            int height = mTopPlayer.getLayoutParams().height;
            mRecyclerView.setTranslationY(height);

            int left = mRecyclerView.getPaddingLeft();
            int right = mRecyclerView.getPaddingRight();
            int top = mRecyclerView.getPaddingTop();

            mRecyclerView.setPadding(left, top, right, mRecyclerViewBottomPadding + height);
        }
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {

        if (mTopPlayer == null) {
            mTopPlayer = (TopPlayer) parent.findViewById(R.id.top_player);
        }

        if (mRecyclerView == null) {
            mRecyclerView = (RecyclerView) parent.findViewById(R.id.my_recycler_view);
            if (mRecyclerView != null) {
                mRecyclerViewBottomPadding = mRecyclerView.getPaddingBottom();
            }
        }

        if (mPlaylistContainerWrapper == null) {
            mPlaylistContainerWrapper = (PlaylistContainerWrapper) parent;
        }

        if (mTopPlayer != null && mRecyclerView != null) {
            int height = (int) mTopPlayer.getPlayerHeight();
            mRecyclerView.setTranslationY(height);

            if (DEBUG) {
                Log.v(TAG, "onLayoutChild, child: " + child.getClass().getName() + " layoutDirection: " + layoutDirection + " height: " + height);
            }
        }

        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        if (DEBUG) {
            Log.v(TAG, "onStartNestedScroll, child: " + child.getClass().getName() + " target: " + target.getClass().getName());
        }

        if (mScroller != null && !mScroller.isFinished()) {
            mScroller.abortAnimation();
        }

        if (mRecyclerView != null) {
            mRecyclerView.canScrollVertically(-1);
        }
        return true;
    }

    @Override
    public void	onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        if (DEBUG)
            Log.v(TAG, "onNestedPreScroll, child: " + child.getClass().getName() + " target: " + target.getClass().getName());

        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (DEBUG)
            Log.v(TAG, "onNestedScroll, child: " + child.getClass().getName() + " target: " + target.getClass().getName() + " dyC: " + dyConsumed + " dyUC: " + dyUnconsumed);

        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY) {
        if (DEBUG)
            Log.v(TAG, "onNestedPreFling, child: " + child.getClass().getName() + " target: " + target.getClass().getName() + "vy: " + velocityY);

        int ymin = 0;
        int ymax = 100000;
        fling(coordinatorLayout, child, ymin, ymax, velocityY);
        return true;
    }

    @Override
    public boolean onNestedFling (CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY, boolean consumed) {
        if (DEBUG)
            Log.v(TAG, "onNestedFling, child: " + child.getClass().getName() + " target: " + target.getClass().getName() + "vy: " + velocityY + " consumed:" + consumed);
        int ymin = 0;
        int ymax = 100000;
        fling(coordinatorLayout, child, ymin, ymax, velocityY);
        return true;
    }

    private boolean fling(CoordinatorLayout coordinatorLayout, View layout, int minOffset, int maxOffset, float velocityY) {
        if(this.mFlingRunnable != null) {
            layout.removeCallbacks(this.mFlingRunnable);
        }

        if(this.mScroller == null) {
            this.mScroller = ScrollerCompat.create(layout.getContext());
        }

        mPlaylistContainerWrapper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        mPlaylistContainerWrapper.dispatchNestedPreFling(0, velocityY);
        mPlaylistContainerWrapper.dispatchNestedFling(0, velocityY, true);

        this.mScroller.fling(0, MAX_VELOCITY_Y, 0, Math.round(velocityY), 0, 0, minOffset, maxOffset);
        if(this.mScroller.computeScrollOffset()) {
            this.mFlingRunnable = new PlaylistBehavior.FlingRunnable(coordinatorLayout, layout);
            ViewCompat.postOnAnimation(layout, this.mFlingRunnable);
            return true;
        } else {
            this.mFlingRunnable = null;
            return false;
        }
    }

    class FlingRunnable implements Runnable {
        private final CoordinatorLayout mParent;
        private final View mLayout;

        private int lastY = 0;

        FlingRunnable(CoordinatorLayout parent, View layout) {
            this.mParent = parent;
            this.mLayout = layout;
        }

        public void run() {
            if(this.mLayout != null &&
                    mTopPlayer != null &&
                    mRecyclerView != null &&
                    PlaylistBehavior.this.mScroller != null &&
                    PlaylistBehavior.this.mScroller.computeScrollOffset()) {

                int currY = PlaylistBehavior.this.mScroller.getCurrY();

                if (lastY != 0) {
                    int diffY = currY-lastY;

                    if (DEBUG)
                        Log.v(TAG, "scroll.run, diffY: " + diffY);

                    if (diffY < 0) {
                        if (mRecyclerView.canScrollVertically(DOWN)) {
                            // it cannot be larger, scroll recyclerview
                            if (DEBUG)
                                Log.v(TAG, "scroll.run, mRecyclerView1");
                            PlaylistBehavior.this.mRecyclerView.scrollBy(0, diffY);
                        } else {
                            if (DEBUG)
                                Log.v(TAG, "scroll.run, mTopPlayer2");
                            PlaylistBehavior.this.mTopPlayer.scrollExternal(diffY, false);
                        }
                    } else {
                        if (mTopPlayer.isMinimumSize()) {
                            if (DEBUG)
                                Log.v(TAG, "scroll.run, mRecyclerView2");
                            PlaylistBehavior.this.mRecyclerView.scrollBy(0, diffY);
                        } else {

                            int canscrollup = canScrollUp(PlaylistBehavior.this.mRecyclerView);
                            if (canscrollup > 0) {
                                Log.v(TAG, "scroll.run, mTopPlayer1 diffY: " + diffY + " canscroll: " + canscrollup);

                                diffY = diffY < canscrollup ? diffY : canscrollup;
                                PlaylistBehavior.this.mTopPlayer.scrollExternal(diffY, false);
                            }
                        }
                    }
                }

                lastY = currY;
                ViewCompat.postOnAnimation(this.mLayout, this);
            }

        }
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

}
