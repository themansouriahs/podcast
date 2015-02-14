package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by apl on 14-02-2015.
 */
public class FloatingView extends RelativeLayoutWithBackground {

    private Context mContext;
    private GestureDetectorCompat mDetector;

    public FloatingView(Context context) {
        super(context);
        init(context);
    }

    public FloatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FloatingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(@NonNull Context argContext) {
        mContext = argContext;
        mDetector = new GestureDetectorCompat(mContext, new MyGestureListener(this));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("INTERCEPT2", "onFling: " + ev.toString());
        //mDetector.onTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d("INTERCEPT2 TOUCH", "onFling: " + ev.toString());
        mDetector.onTouchEvent(ev);
        return true;
        //return super.onTouchEvent(ev);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        private View mView;

        public MyGestureListener(@NonNull View argView) {
            mView = argView;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(DEBUG_TAG, "onScroll: e1" + e1.toString() + " e2: " + e2.toString());
            translateY(distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
            return true;
        }

        private void translateY(float distanceY) {
            float newOffset = -distanceY; // mView.getTranslationY() +
            Log.d("ttt", "trans: " + mView.getTranslationY() + " dy: " + distanceY + " new: " + newOffset);

            mView.setTranslationY(newOffset);
        }
    }
}
