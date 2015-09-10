package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Debug;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import org.bottiger.podcast.adapters.decoration.OnDragStateChangedListener;

/**
 * Created by apl on 22-08-2014.
 *
 * http://stackoverflow.com/questions/25178329/recyclerview-and-swiperefreshlayout
 */
public class FixedRecyclerView extends RecyclerView {

    public FixedRecyclerView(Context context) {
        super(context);
    }

    public FixedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
