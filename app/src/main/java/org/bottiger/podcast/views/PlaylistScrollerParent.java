package org.bottiger.podcast.views;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by aplb on 09-09-2015.
 */
public class PlaylistScrollerParent extends LinearLayout {

    private Context mContext;
    private AttributeSet mAttributeSet = null;

    public PlaylistScrollerParent(Context context) {
        super(context);
        mContext = context;
    }

    public PlaylistScrollerParent(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAttributeSet = attrs;
    }

    public PlaylistScrollerParent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mAttributeSet = attrs;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        //assert getLayoutParams() != null;


        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)getLayoutParams();
        CoordinatorLayout.Behavior behavior = new PlaylistBehavior(mContext, mAttributeSet);
        lp.setBehavior(behavior);
        setLayoutParams(lp);

    }
}
