package org.bottiger.podcast.views;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;

import org.bottiger.podcast.DrawerActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.TopActivity;

/**
 * Created by apl on 17-01-2015.
 */
public class ToolbarActivity extends TopActivity {


    protected Toolbar mToolbar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_swipe);

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);

        // if we can use windowTranslucentNavigation=true
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams();
            params.topMargin = getStatusBarHeight();
            mToolbar.setLayoutParams(params);
        }

        //Title and subtitle
        mToolbar.setTitle(getResources().getString(R.string.app_name));
        //mToolbar.setSubtitle("Subtitle");

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
