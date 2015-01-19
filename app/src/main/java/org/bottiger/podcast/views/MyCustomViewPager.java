package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by apl on 13-09-2014.
 */
public class MyCustomViewPager extends ViewPager {
    public MyCustomViewPager(Context context) {
        super(context);
    }

    public MyCustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        return false;
    }

    /*
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        String tag = "MyCustomViewPager intercept";
        Log.d(tag, "------------");
        Log.d(tag, ev.toString());
        Log.d(tag, "------------");
        return super.onInterceptTouchEvent(ev);
    }*/

    /*
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }*/
}
