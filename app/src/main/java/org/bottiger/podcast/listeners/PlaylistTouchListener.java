package org.bottiger.podcast.listeners;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

import org.bottiger.podcast.adapters.decoration.DragSortRecycler;
import org.bottiger.podcast.adapters.decoration.OnDragStateChangedListener;
import org.bottiger.podcast.views.ExpandableLayoutManager;
import org.bottiger.podcast.views.FixedRecyclerView;
import org.bottiger.podcast.views.SwipeRefreshExpandableLayout;
import org.bottiger.podcast.views.TopPlayer;

/**
 * Created by apl on 13-01-2015.
 */
public class PlaylistTouchListener extends GestureDetector.SimpleOnGestureListener implements OnDragStateChangedListener {

    private SwipeRefreshExpandableLayout mSwipeRefreshView;
    private TopPlayer mTopPlayer;
    private FixedRecyclerView mRecyclerView;
    private ImageView mPhoto;

    public static boolean usingLargeLayout = true;

    public PlaylistTouchListener(@NonNull SwipeRefreshExpandableLayout argSwipeRefreshExpandableLayout,
                                 @NonNull TopPlayer argTopPlayer,
                                 @NonNull FixedRecyclerView argFixedRecyclerView,
                                 @NonNull ImageView argImageView) {
        mSwipeRefreshView =argSwipeRefreshExpandableLayout;
        mTopPlayer = argTopPlayer;
        mRecyclerView = argFixedRecyclerView;
        mPhoto = argImageView;
    }

    @Override
    public boolean onSingleTapUp (MotionEvent e) {
        Log.d("GeatureDetector", "onSingleTapUp: e -> " + e);
        if (e.getY() < mTopPlayer.getVisibleHeight()) {
            Log.d("GeatureDetector", "mTopPlayer.onTouchEvent(e)");
            return mTopPlayer.onTouchEvent(e);
        } else {
            Log.d("GeatureDetector", "mRecyclerView.onTouchEvent(e)");
            return mRecyclerView.onTouchEvent(e);
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (e1==null || e2==null || mRecyclerView.getChildAt(0) == null) {
            return false;
        }

        if (isDradding)
            return false;

        Log.d("GeatureDetector", "onScroll: e1y -> " + e1.getY() + " e2y -> " + e2.getY() + " dy -> " + distanceY);
        mSwipeRefreshView.getOverScroller().startScroll((int)e2.getX(),
                (int)e2.getY(),
                (int)distanceX,
                (int)distanceY);

        boolean minimalPlayer = mSwipeRefreshView.getCurrentScrollState() == SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER
                || mSwipeRefreshView.getCurrentScrollState() == SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST;
        Log.d("GeatureDetector", "minimalplayer: -> " + minimalPlayer);

        // distanceY > 0 => scroll up
        // distanceY < 0 => scroll down
        int containerHeight = mTopPlayer.getHeight();
        float containerTranslationY = mTopPlayer.getTranslationY();
        //float currentHeight = !mTopPlayer.isMinimumSize() ? containerHeight+containerTranslationY-distanceY : mTopPlayer.getHeight()-distanceY;
        float currentHeight = containerHeight+containerTranslationY-distanceY;

        Log.d("GeatureDetector", "currentHeight: -> " + currentHeight + " getTop: " + mRecyclerView.getChildAt(0).getTop() + " mTopPlayer.isMinimumSize(): " + mTopPlayer.isMinimumSize((int) currentHeight));

        boolean newHeightSmallerThanMinimum = mTopPlayer.isMinimumSize((int)currentHeight);
        if (minimalPlayer) {
            if (distanceY > 0 && newHeightSmallerThanMinimum ||
                    distanceY < 0 && mRecyclerView.getChildAt(0).getTop() != 0) {

                ExpandableLayoutManager elm = ((ExpandableLayoutManager) mRecyclerView.getLayoutManager());
                elm.SetCanScrollVertically(true);


                Log.d("GeatureDetector", "onScroll recyclerview: -> " + distanceY);
                mRecyclerView.scrollBy(0, (int) distanceY);
                //mRecyclerView.scrollToPosition(6);

                return true;
            }
        }

        return scrollLayout(distanceY);
    }

    // http://stackoverflow.com/questions/4951142/smooth-scrolling-in-android
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d("GeatureDetector", "Fling : e1y -> " + e1.getY() + " e2y -> " + e2.getY() + " vy -> " + velocityY);
        //mSwipeRefreshView.getOverScroller().
        mSwipeRefreshView.getOverScroller().fling((int)e1.getX(), (int)e1.getY(),
                (int)velocityX, (int)velocityY, 0, (int)10000, 0, (int)10000);

        /*
        float minSize = mTopPlayer.getMinimumSize();
        float trans = minSize-mTopPlayer.getHeight();

        float newVisibleHeight = mTopPlayer.setPlayerHeight(minSize, trans);
        mRecyclerView.setTranslationY(newVisibleHeight);
        mRecyclerView.bringToFront();
        */

        /*
        ExpandableLayoutManager elm = ((ExpandableLayoutManager) mRecyclerView.getLayoutManager());

        elm.SetCanScrollVertically(true);

        return mRecyclerView.fling((int)velocityX, (int)velocityY);
        */
        return true;
    }

    public boolean scrollLayout( float distanceY) {
        Log.d("GeatureDetector", "scrollLayout: distanceY -> " + distanceY);

        ExpandableLayoutManager elm = ((ExpandableLayoutManager) mRecyclerView.getLayoutManager());
        if (mTopPlayer.isMinimumSize()) {
            elm.SetCanScrollVertically(true);
        } else {
            elm.SetCanScrollVertically(false);
        }

        mSwipeRefreshView.mDownGeastureInProgress = true;
        // distanceY > 0 => scroll up
        // distanceY < 0 => scroll down

        if (mTopPlayer.isMinimumSize() && distanceY > 0) {
            mTopPlayer.bringToFront();
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
                mTopPlayer.bringToFront();
                usingLargeLayout = false;
            }
        } else {
            if (!usingLargeLayout) {
                mRecyclerView.bringToFront();
                mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.PARTIAL_PLAYER);
            }
            usingLargeLayout = true;
        }

        boolean isMinimumSize = mTopPlayer.isMinimumSize((int)newVisibleHeight);

        mSwipeRefreshView.setCanScrollRecyclerView(isMinimumSize);
        mRecyclerView.setCanScrollRecyclerView(isMinimumSize);

        if (isMinimumSize) {
            Log.d("TopPlayerIOut", "To the top!");
            mTopPlayer.bringToFront();
            //mRecyclerView.scrollToPosition(0);
        }

        int[] location = new int[2];
        mRecyclerView.getChildAt(0).getLocationOnScreen(location);

        int[] location2 = new int[2];
        mRecyclerView.getLocationOnScreen(location2);

        Log.d("TopPlayerIOut", "Translation =>" + newVisibleHeight + " mRecyclerView.child.Top => " + location[0] + " & " + location[1]);
        Log.d("TopPlayerIOut", "diffx =>" + (location[0]-location2[0]) + " diffy => " + (location[1]-location2[1]));
        mRecyclerView.setTranslationY(newVisibleHeight);

        Log.d("TopPlayerInputkmRecyclerView", "mRecyclerView translation ->" + mRecyclerView.getTranslationY());
        return true;
    }

    private boolean isDradding = false;

    @Override
    public void onDragStart(int position) {
        isDradding = true;
    }

    @Override
    public void onDragStop(int position) {
        isDradding = false;
    }
}
