package org.bottiger.podcast.Animations;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.ThemeHelper;

/**
 * Created by apl on 09-09-2014.
 */
public class DepthPageTransformer implements ViewPager.PageTransformer {
    private static final float MIN_SCALE = 0.75f;
    private static final float THRESHOLD = 0.01f;

    private boolean mInitializedBackgroundColor = false;
    private @ColorInt int mBackgroundColor = 0;

    private View mParentView;

    public DepthPageTransformer(@Nullable View argParentView) {
        mParentView = argParentView;
    }

    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (Math.abs(position-Math.round(position)) < THRESHOLD) {
                mParentView.setBackground(null);
            } else {
                setParentBackground();
            }
        }

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(0);

        } else if (position <= 0) { // [-1,0]
            // Use the default slide transition when moving to the left page
            view.setAlpha(1);
            view.setTranslationX(0);
            view.setScaleX(1);
            view.setScaleY(1);

        } else if (position <= 1) { // (0,1]
            // Fade the page out.
            view.setAlpha(1 - position);

            // Counteract the default slide transition
            view.setTranslationX(pageWidth * -position);

            // Scale the page down (between MIN_SCALE and 1)
            float scaleFactor = MIN_SCALE
                    + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(0);
        }
    }

    private void setParentBackground() {
        if (mParentView== null)
            return;

        if (mInitializedBackgroundColor) {
            setBackground(mBackgroundColor);
            return;
        }

        Context context = mParentView.getContext();
        ThemeHelper themeHelper = new ThemeHelper(context);
        int colorRes = themeHelper.getAttr(R.attr.themeBackground);

        int color = context.getResources().getColor(colorRes);
        mBackgroundColor = color;
        mInitializedBackgroundColor = true;

        mParentView.setBackgroundColor(color);
    }

    private void setBackground(@ColorInt int argColor) {
        Drawable background = mParentView.getBackground();
        if (background == null) {
            mParentView.setBackgroundColor(argColor);
        }
    }
}
