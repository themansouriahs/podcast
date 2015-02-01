package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerTitleStrip;
import android.support.v7.widget.Toolbar;
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

    private PagerTitleStrip mPagerTitleStrip;
    private View mAppContent;

    private Drawable mToolBackground = null;
    private Drawable mPagerTitleStripBackground = null;

    private int mToolBackgroundColor = -1;
    private int mPagerTitleStripBackgroundColor = -1;

    protected Toolbar mToolbar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_swipe);

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        mPagerTitleStrip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
        mAppContent = findViewById(R.id.app_content);

        // if we can use windowTranslucentNavigation=true
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams();
            params.topMargin = getStatusBarHeight();
            mToolbar.setLayoutParams(params);
        }

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
    protected void makeToolbarTransparent(boolean argIsTransparent) {
        // If the user API level is less that 17 we just keep the toolbar
        if (Build.VERSION.SDK_INT < 17)
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
        }

        if (!argIsTransparent) {
            mToolbar.setBackgroundColor(mToolBackgroundColor);
            mPagerTitleStrip.setBackgroundColor(mPagerTitleStripBackgroundColor);
        } else {
            int alpha  = 0;
            mToolbar.getBackground().setAlpha(alpha);
            mPagerTitleStrip.getBackground().setAlpha(alpha);
            //mToolbar.setBackgroundColor(mPagerTitleStripBackgroundColor);
            //mPagerTitleStrip.setBackgroundColor(mToolBackgroundColor);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAppContent.getLayoutParams();

        if (argIsTransparent) {
            params.removeRule(RelativeLayout.BELOW);
            mToolbar.bringToFront();
            mPagerTitleStrip.bringToFront();
        } else
            params.addRule(RelativeLayout.BELOW, R.id.my_awesome_toolbar);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
