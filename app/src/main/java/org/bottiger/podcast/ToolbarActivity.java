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
import android.support.design.widget.TabLayout;
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
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;

/**
 * Created by apl on 17-01-2015.
 */
public class ToolbarActivity extends TopActivity {

    private boolean mIsTransparant = false;

    private View mPagerTitleStrip;
    private View mAppContent;
    protected TabLayout mPagerTaps;

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
        mPagerTaps = (TabLayout) findViewById(R.id.tabs);

        mAppContent = findViewById(R.id.app_content);

        //Title and subtitle
        mToolbar.setTitle(getResources().getString(R.string.app_name));

        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar == null) {
            VendorCrashReporter.report("Actionbarnull", "yep, it's true");
            return;
        }

        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeButtonEnabled(true);


        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt == Build.VERSION_CODES.KITKAT) {
            int paddingTop = mToolbar.getPaddingTop();
            int paddingLeft = mToolbar.getPaddingLeft();
            int paddingRight = mToolbar.getPaddingRight();
            int paddingBottom = mToolbar.getPaddingBottom();
            mToolbar.setPadding(paddingLeft, paddingTop + getStatusBarHeight(getResources()), paddingRight, paddingBottom);
        }
    }

    public static int getNavigationBarHeight(Resources res) {
        int result = 0;
        int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getStatusBarHeight(Resources res) {
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
