package org.bottiger.podcast.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class SquareImageViewFresco extends ImageViewTinted {
    public SquareImageViewFresco(Context context) {
        super(context);
    }

    public SquareImageViewFresco(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageViewFresco(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
