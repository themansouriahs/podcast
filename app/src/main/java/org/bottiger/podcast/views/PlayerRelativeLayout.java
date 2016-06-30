package org.bottiger.podcast.views;

import android.content.Context;
import android.util.AttributeSet;

public class PlayerRelativeLayout extends android.support.percent.PercentRelativeLayout {

    public PlayerRelativeLayout(Context context) {
        super(context);
    }

    public PlayerRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setHeight(int argHeight) {
        if (argHeight < 0) {
            throw new IllegalArgumentException("Height may not be negative");
        }

        this.getLayoutParams().height = argHeight;
    }
}
