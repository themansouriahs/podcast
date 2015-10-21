package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.bottiger.podcast.R;

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
