package org.bottiger.podcast;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerTitleStrip;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.bottiger.podcast.DrawerActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.TopActivity;

/**
 * Created by apl on 17-01-2015.
 */
public class ToolbarActivity extends TopActivity {

    private boolean mIsTransparant = false;

    private View mPagerTitleStrip;
    private View mAppContent;

    private Drawable mToolBackground = null;
    private Drawable mPagerTitleStripBackground = null;

    private int mToolBackgroundColor = -1;
    private int mPagerTitleStripBackgroundColor = -1;

    ValueAnimator mColorAnimation;

    protected Toolbar mToolbar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayout());

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        mPagerTitleStrip = (View) findViewById(R.id.sliding_tabs);

        mAppContent = findViewById(R.id.app_content);

        // if we can use windowTranslucentNavigation=true
        /*
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams();
            params.topMargin = getStatusBarHeight();
            mToolbar.setLayoutParams(params);
        }*/

        //Title and subtitle
        mToolbar.setTitle(getResources().getString(R.string.app_name));

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /**
     *
     * @param argIsTransparent
     */
    @TargetApi(17)
    public void makeToolbarTransparent(boolean argIsTransparent) {

        if (argIsTransparent)
            return;

        if (mToolBackground == null) {
            mToolBackground = mToolbar.getBackground();
            mPagerTitleStripBackground = mPagerTitleStrip.getBackground();

            TypedArray array = getTheme().obtainStyledAttributes(new int[] {
                    android.R.attr.colorPrimary,
                    android.R.attr.colorAccent,
            });
            mToolBackgroundColor = array.getColor(0, 0xFF00FF);
            mPagerTitleStripBackgroundColor = array.getColor(1, 0xFF00FF);
            array.recycle();

            //mToolbar.setBackgroundColor(mToolBackgroundColor);
            //mPagerTitleStrip.setBackgroundColor(mPagerTitleStripBackgroundColor);
        }

        // If the user API level is less that 17 we just keep the toolbar
        if (Build.VERSION.SDK_INT < 17)
            return;

        if (mIsTransparant == argIsTransparent)
            return;

        mIsTransparant = argIsTransparent;

        int minAlpha = argIsTransparent ? 255 : 0;
        int maxAlpha = argIsTransparent ? 0 : 255;

        ObjectAnimator anim = ObjectAnimator.ofFloat(mToolbar.getBackground(), "alpha", minAlpha, maxAlpha);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(mPagerTitleStrip.getBackground(), "alpha", minAlpha, maxAlpha);
        anim.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        anim2.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));

        mColorAnimation = ValueAnimator.ofInt(minAlpha, maxAlpha);
        mColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                //textView.setBackgroundColor((Integer)animator.getAnimatedValue());
                Integer alphaValue = (Integer)animator.getAnimatedValue();

                mToolbar.getBackground().mutate().setAlpha(alphaValue);
                mPagerTitleStrip.getBackground().mutate().setAlpha(alphaValue);

                Log.d("AlphaAnimator", "alpha: " + alphaValue + " bgAlpha: " + mPagerTitleStrip.getBackground().getOpacity());
            }

        });
        mColorAnimation.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        mColorAnimation.start();
    }

    protected static int getStatusBarHeight(Resources res) {
        int result = 0;
        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    protected int getLayout() {
        return R.layout.activity_swipe;
    }
}
