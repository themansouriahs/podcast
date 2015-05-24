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

import org.bottiger.podcast.PlaylistFragment;
import org.bottiger.podcast.R;

/**
 * Created by apl on 31-07-2014.
 */
public class SwipeRefreshExpandableLayout extends FeedRefreshLayout {

    private Context mContext;
    private GestureDetector mDetector;
    private OverScroller mScroller;

    private FixedRecyclerView mRecycerView;
    private TopPlayer mTopPlayer;

    public PlaylistFragment fragment = null;

    public boolean mDownGeastureInProgress = false;

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


    public View mTarget;


    public boolean canChildScrollUp() {
        return true;
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

            lastScrollPos = currentScrollPos;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.d("SwipeRefreshExpandable", "event" + event.toString());
        switch (event.getAction())
        {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDownGeastureInProgress = false;
                break;
        }

        //delegateToTopPlayer(event);

        boolean TouchFromSuper = super.onTouchEvent(event);

        if (mDownGeastureInProgress) {
            Log.d("SwipeRefreshExpandable", "(mDownGeastureInProgress) return: " + true);
            return true;
        }

        Log.d("SwipeRefreshExpandable", "return: " + TouchFromSuper);
        //return TouchFromSuper;
        return true;
    }

}
