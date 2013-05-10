package org.bottiger.podcast.views;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class SquareGridViewGroup extends ViewGroup {

    public SquareGridViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
     // Do nothing   
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    } 

}
