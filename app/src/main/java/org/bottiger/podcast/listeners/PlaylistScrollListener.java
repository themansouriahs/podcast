package org.bottiger.podcast.listeners;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.Player.PlayerPhoneListener;
import org.bottiger.podcast.adapters.decoration.DragSortRecycler;
import org.bottiger.podcast.adapters.decoration.OnDragStateChangedListener;
import org.bottiger.podcast.views.ExpandableLayoutManager;
import org.bottiger.podcast.views.FixedRecyclerView;
import org.bottiger.podcast.views.SwipeRefreshExpandableLayout;
import org.bottiger.podcast.views.TopPlayer;

import java.util.WeakHashMap;

/**
 * Created by apl on 23-01-2015.
 */
public class PlaylistScrollListener extends RecyclerView.OnScrollListener implements OnDragStateChangedListener {

    private SwipeRefreshExpandableLayout mSwipeRefreshView;
    private TopPlayer mTopPlayer;
    private FixedRecyclerView mRecyclerView;
    private ImageView mPhoto;

    public static boolean usingLargeLayout = true;
    private boolean isDradding = false;

    private WeakHashMap<PostScrollListener, Boolean> mPostScrollListeners = new WeakHashMap<>();

    public interface PostScrollListener {
        public void hasScrolledY(int dy);
    }

    public PlaylistScrollListener(@Nullable PostScrollListener argPostScrollListener) {
        if (argPostScrollListener != null) {
            mPostScrollListeners.put(argPostScrollListener, true);
        }
    }

    public PlaylistScrollListener(@Nullable PostScrollListener argPostScrollListener,
                              @NonNull SwipeRefreshExpandableLayout argSwipeRefreshExpandableLayout,
                              @NonNull TopPlayer argTopPlayer,
                              @NonNull FixedRecyclerView argFixedRecyclerView,
                              @NonNull ImageView argImageView) {
        this(argPostScrollListener);
        mSwipeRefreshView =argSwipeRefreshExpandableLayout;
        mTopPlayer = argTopPlayer;
        mRecyclerView = argFixedRecyclerView;
        mPhoto = argImageView;
    }

    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (ApplicationConfiguration.DEBUGGING)
            Log.d("PlaylistScrollListener", "Scrolled: " + dx + " " + dy);

        for(PostScrollListener listener : mPostScrollListeners.keySet()) {
            listener.hasScrolledY(dy);
        }
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

    @Override
    public void onDragStart(int position) {
        isDradding = true;
    }

    @Override
    public void onDragStop(int position) {
        isDradding = false;
    }
}
