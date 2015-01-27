package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

/**
 * Created by apl on 31-07-2014.
 */
public class ExpandableLayoutManager extends LinearLayoutManager {

    private SwipeRefreshExpandableLayout mSwipeRefreshView;
    private TopPlayer mTopPlayer;
    private FixedRecyclerView mRecyclerView;
    private ImageView mPhoto;

    public static boolean usingLargeLayout = true;

    public ExpandableLayoutManager( @NonNull Context argContext,
                                  @NonNull SwipeRefreshExpandableLayout argSwipeRefreshExpandableLayout,
                                  @NonNull TopPlayer argTopPlayer,
                                  @NonNull FixedRecyclerView argFixedRecyclerView,
                                  @NonNull ImageView argImageView) {
        super(argContext);
        mSwipeRefreshView =argSwipeRefreshExpandableLayout;
        mTopPlayer = argTopPlayer;
        mRecyclerView = argFixedRecyclerView;
        mPhoto = argImageView;
    }

    public TopPlayer getTopPlayer() {
        return mTopPlayer;
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


        boolean minimalPlayer = mSwipeRefreshView.getCurrentScrollState() == SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER
                || mSwipeRefreshView.getCurrentScrollState() == SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST;
        Log.d("GeatureDetector", "minimalplayer: -> " + minimalPlayer);

        // distanceY > 0 => scroll up
        // distanceY < 0 => scroll down
        int containerHeight = mTopPlayer.getHeight();
        float containerTranslationY = mTopPlayer.getTranslationY();
        float currentHeight = containerHeight+containerTranslationY-dy;

        Log.d("GeatureDetector", "currentHeight: -> " + currentHeight + " getTop: " + mRecyclerView.getChildAt(0).getTop() + " mTopPlayer.isMinimumSize(): " + mTopPlayer.isMinimumSize((int) currentHeight));

        boolean newHeightSmallerThanMinimum = mTopPlayer.isMinimumSize((int)currentHeight);

        int amount = 0;

        ///////////////////////////
        boolean hasSCrolled = false;
        if (minimalPlayer) {
            if (dy > 0 && newHeightSmallerThanMinimum ||
                    dy < 0 && mRecyclerView.getChildAt(0).getTop() != 0) {

                Log.d("GeatureDetector", "onScroll recyclerview: -> " + dy);

                hasSCrolled = true;
                mTopPlayer.ensureMinimalLayout();
                amount = super.scrollVerticallyBy(dy, recycler, state);
            }
        }

        if (!hasSCrolled) {
            scrollLayout(dy);
        }


        return amount;
    }

    public boolean scrollLayout( float distanceY) {
        Log.d("GeatureDetector", "scrollLayout: distanceY -> " + distanceY);

        mSwipeRefreshView.mDownGeastureInProgress = true;
        // distanceY > 0 => scroll up
        // distanceY < 0 => scroll down

        if (mTopPlayer.isMinimumSize() && distanceY > 0) {
            mTopPlayer.ensureMinimalLayout();
            mSwipeRefreshView.mDownGeastureInProgress = false;
            mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST);
            return false;
        }

        int containerHeight = mTopPlayer.getHeight();
        float containerTranslationY = mTopPlayer.getTranslationY();
        //float currentHeight = !mTopPlayer.isMinimumSize() ? containerHeight+containerTranslationY-distanceY : mTopPlayer.getHeight()-distanceY;
        float currentHeight = containerHeight+containerTranslationY-distanceY;

        Log.d("setTranslationXYZ", "h -> " + containerHeight + " t -> " + containerTranslationY + " dy -> " + distanceY + " curH -> " + currentHeight);



        float newShrinkAmount = mTopPlayer.getTranslationY()-distanceY;
        Log.d("setTranslationXYZ", "newShrinkAmount: " +  newShrinkAmount + " trans: " + mTopPlayer.getTranslationY() + " distY: " + distanceY);

        float newVisibleHeight = mRecyclerView.getTranslationY();

        // Prevent the user from scrolling too far down. (i.e. more down than the maximum size player)
        if (mTopPlayer.isMaximumSize() && currentHeight >= 0 && newShrinkAmount > 0) {
            mTopPlayer.ensureMaximumLayout();
            mPhoto.setTranslationY(0);
            mRecyclerView.setTranslationY(mTopPlayer.getHeight());
            mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.FULL_PLAYER);
            //mTopPlayer.setPlayerHeight(mTopPlayer.get, newOffset);
            return false;
        }

        if ( (!mTopPlayer.isMinimumSize() && distanceY > 0) || (!mTopPlayer.isMaximumSize() && distanceY < 0) ) {
            newVisibleHeight = mTopPlayer.setPlayerHeight(currentHeight, newShrinkAmount);
            Log.d("photoOffsejjjt", "newShrinkAmount (in) -> "  + newShrinkAmount + " newVisibleHeight (out) -> " + newVisibleHeight);
        }

        if (mTopPlayer.isMinimumSize()) {
            if (usingLargeLayout) {
                Log.d("PlaylistHeight", "TopPlayer is minimum");
                mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER);
                usingLargeLayout = false;
            }
        } else {
            if (!usingLargeLayout) {
                mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.PARTIAL_PLAYER);
            }
            usingLargeLayout = true;
        }

        boolean isMinimumSize = mTopPlayer.isMinimumSize((int)newVisibleHeight);

        mSwipeRefreshView.setCanScrollRecyclerView(isMinimumSize);

        if (isMinimumSize) {
            Log.d("TopPlayerIOut", "To the top!");
            //mRecyclerView.scrollToPosition(0);
        }

        int[] location = new int[2];
        mRecyclerView.getChildAt(0).getLocationOnScreen(location);

        int[] location2 = new int[2];
        mRecyclerView.getLocationOnScreen(location2);

        Log.d("TopPlayerIOut", "Translation =>" + newVisibleHeight + " mRecyclerView.child.Top => " + location[0] + " & " + location[1]);
        Log.d("TopPlayerIOut", "diffx =>" + (location[0]-location2[0]) + " diffy => " + (location[1]-location2[1]));
        mRecyclerView.setTranslationY(newVisibleHeight);

        Log.d("TopPlayerInputk", "mRecyclerView translation ->" + mRecyclerView.getTranslationY());

        //mRecyclerView.invalidate();
        return true;
    }

}
