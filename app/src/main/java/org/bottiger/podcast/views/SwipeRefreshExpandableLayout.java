package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.bottiger.podcast.PlaylistFragment;
import org.bottiger.podcast.R;

/**
 * Created by apl on 31-07-2014.
 */
public class SwipeRefreshExpandableLayout extends FeedRefreshLayout implements Target {

    private Context mContext;
    private GestureDetector mDetector;
    private OverScroller mScroller;

    private FixedRecyclerView mRecycerView;
    private TopPlayer mTopPlayer;

    public PlaylistFragment fragment = null;

    public boolean mDownGeastureInProgress = false;
    private boolean mCanScrollRecyclerView = false;

    private boolean mRequestDisallowInterceptTouchEvent = false;

    private ScrollState mCurrentScrollState = ScrollState.FULL_PLAYER;

    public static enum ScrollState { FULL_PLAYER, PARTIAL_PLAYER, MINIMAIL_PLAYER, MINIMAIL_PLAYER_AND_SCROLLED_LIST }

    public SwipeRefreshExpandableLayout(Context context) {
        super(context);
        init(context);
    }

    public SwipeRefreshExpandableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context argContext) {
        mContext = argContext;
        mScroller = new OverScroller(mContext);
    }

    @Override
    protected void onFinishInflate () {
        mRecycerView = (FixedRecyclerView) findViewById(R.id.my_recycler_view);
        mTopPlayer = (TopPlayer) findViewById(R.id.session_photo_container);
    }

    public ScrollState getCurrentScrollState() {
        return mCurrentScrollState;
    }

    public void setCurrentScrollState(ScrollState argScrollState) {
        mCurrentScrollState = argScrollState;
    }

    public void setGestureListener(GestureDetector.OnGestureListener listener) {
        mDetector = new GestureDetector(mContext, listener);
    }

    public OverScroller getOverScroller() {
        return mScroller;
    }

    public View mTarget;


    //@Override
    public boolean canChildScrollUp() {
        return true;
    }

    private void delegateToTopPlayer(MotionEvent event) {
        if (mRecycerView != null) { // && (mRecycerView.scrolledToTop() || (mCurrentScrollState != ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST))) {
            if (!FixedRecyclerView.mSeekbarSeeking) {
                Log.d("Delegate touch event", "event -> " + event.toString());
                mDetector.onTouchEvent(event);
            }
        }
    }

    private int lastScrollPos = 0;
    @Override
    protected void onDraw(Canvas canvas) {

        // scrollTo invalidates, so until animation won't finish it will be called
        // (used after a Scroller.fling() )
        if(mScroller.computeScrollOffset()) {
            int currentScrollPos = mScroller.getCurrY();
            int diff = lastScrollPos-currentScrollPos;
            Log.d("GeatureDetector", "current fling pos => " + currentScrollPos + " last fling pos =>" + lastScrollPos + " diff => " + diff);

            if (mCurrentScrollState == ScrollState.MINIMAIL_PLAYER) {
                Log.d("ScrollType", "RecyclerView, dy =>" + diff);
                mRecycerView.scrollBy(0,diff);
                mCurrentScrollState = ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST;
            } else {
                Log.d("ScrollType", "TopPlayer, dy => " + diff);
                //fragment.scrollLayout(diff);
            }
            lastScrollPos = currentScrollPos;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.d("SwipeRefreshExpandableLayout touch", "event" + event.toString());
        switch (event.getAction())
        {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDownGeastureInProgress = false;
                break;
        }

        delegateToTopPlayer(event);

        boolean TouchFromSuper = super.onTouchEvent(event);

        if (mDownGeastureInProgress) {
            Log.d("SwipeRefreshExpandableLayout touch", "(mDownGeastureInProgress) return: " + true);
            return true;
        }

        Log.d("SwipeRefreshExpandableLayout touch", "return: " + TouchFromSuper);
        //return TouchFromSuper;
        return true;
    }
    @Override
    public void requestDisallowInterceptTouchEvent(boolean argDisallow) {
        mRequestDisallowInterceptTouchEvent = argDisallow;
        super.requestDisallowInterceptTouchEvent(argDisallow);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {

        delegateToTopPlayer(event);

        switch (event.getAction())
        {
            case MotionEvent.ACTION_MOVE:
                mDownGeastureInProgress = true;
                //return true;
                break;
        }

        /*
        if (mRecycerView != null && mRecycerView.canScrollVertically(FixedRecyclerView.DOWN)) {
            Log.d("SwipeRefreshExpandableLayout touch", "intercept (recycler can scroll) =>" + false);
            return false;
        }

        if (mRecycerView == null) {
            Log.d("SwipeRefreshExpandableLayout touch", "intercept (recycler null) =>" + false);
            return false;
        }*/

        //delegateToTopPlayer(event);

        /*
        if (mCurrentScrollState == ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST && !FixedRecyclerView.mSeekbarSeeking) {
            //return true;
            mRecycerView.setCanScrollRecyclerView(true);
            Log.d("SwipeRefreshExpandableLayout touch", "intercept (can scroll recycler) =>" + false);
            return false;
        }

        if (mRequestDisallowInterceptTouchEvent)
            return false;

        if (mCurrentScrollState != ScrollState.MINIMAIL_PLAYER) {
            mRecycerView.setCanScrollRecyclerView(false);
        }*/

        Log.d("SwipeRefreshExpandableLayout touch", "intercept =>" + mDownGeastureInProgress);
        //return mDownGeastureInProgress;
        return false;
    }

    public boolean getCanScrollRecyclerView() {
        return mCanScrollRecyclerView;
    }

    public void setCanScrollRecyclerView(boolean mCanScrollRecyclerView) {
        this.mCanScrollRecyclerView = mCanScrollRecyclerView;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        //setBackgroundDrawable(new BitmapDrawable(bitmap));
        BitmapDrawable ob = new BitmapDrawable(getResources(),bitmap);
        setBackground(ob);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

}
