package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Switch;

/**
 * Created by apl on 12-02-2015.
 */
public class FeedViewSwitch extends Switch {
    public FeedViewSwitch(Context context) {
        super(context);
        init(context);
    }

    public FeedViewSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FeedViewSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FeedViewSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context argContext) {

    }
}
