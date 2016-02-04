package org.bottiger.podcast.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by aplb on 29-01-2016.
 */
public class SubscriptionRecycler extends RecyclerView {
    public SubscriptionRecycler(Context context) {
        super(context);
    }

    public SubscriptionRecycler(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SubscriptionRecycler(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }
}
