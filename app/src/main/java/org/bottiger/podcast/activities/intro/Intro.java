package org.bottiger.podcast.activities.intro;

import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.view.ViewPager;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.NavigationPolicy;
import com.heinrichreimersoftware.materialintro.app.OnNavigationBlockedListener;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;

/**
 * Created by aplb on 03-12-2016.
 */

public class Intro extends IntroActivity {

    private long mStartTime = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState){

        /* Enable/disable fullscreen */
        setFullscreen(true);

        super.onCreate(savedInstanceState);

        @ColorRes int colorResBg = R.color.colorPrimary;
        @ColorRes int colorRes = R.color.colorBgAccent;

        setButtonBackVisible(false);

        setButtonNextFunction(BUTTON_NEXT_FUNCTION_NEXT);

        /* Add a navigation policy to define when users can go forward/backward */
        setNavigationPolicy(new NavigationPolicy() {
            @Override
            public boolean canGoForward(int position) {
                return true;
            }

            @Override
            public boolean canGoBackward(int position) {
                return position != 0;
            }
        });

        /* Add a listener to detect when users try to go to a page they can't go to */
        addOnNavigationBlockedListener(new OnNavigationBlockedListener() {
            @Override
            public void onNavigationBlocked(int position, int direction) {
            }
        });

        /* Add your own page change listeners */
        addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                boolean isLast = position == getCount() - 1;
                setButtonNextFunction(isLast ? BUTTON_NEXT_FUNCTION_NEXT_FINISH : BUTTON_NEXT_FUNCTION_NEXT);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


        /**
         * The welcome slide
         */
        addSlide(new SimpleSlide.Builder()
                .title(R.string.app_intro_welcome_title)
                .description(R.string.app_intro_welcome_description)
                .image(R.drawable.sw_logo)
                .layout(R.layout.intro_layout)
                .background(colorRes)
                .backgroundDark(colorResBg)
                .build());

        /**
         * The playlist slide
         */
        String title = getString(R.string.appintro_playlist_title);
        String desc = getString(R.string.appintro_playlist_description);
        VideoIntro playlistIntro = VideoIntro.newInstance(title, desc, R.raw.slide1);
        addSlide(new FragmentSlide.Builder()
                .background(R.color.green)
                .backgroundDark(R.color.greenDark)
                .fragment(playlistIntro)
                .build());

        /**
         * Filters slide
         */
        String filters_title = getString(R.string.appintro_filters_title);
        String filters_desc = getString(R.string.appintro_filters_description);
        VideoIntro filtersIntro = VideoIntro.newInstance(filters_title, filters_desc, R.raw.slide2);
        addSlide(new FragmentSlide.Builder()
                .background(R.color.blue)
                .backgroundDark(R.color.darkblue)
                .fragment(filtersIntro)
                .build());

        /**
         * Settings slide
         */
        SettingsIntro settingsIntro = SettingsIntro.newInstance();
        addSlide(new FragmentSlide.Builder()
                .background(R.color.purple)
                .backgroundDark(R.color.purpleDark)
                .fragment(settingsIntro)
                .build());
    }

    @Override
    protected void onDestroy() {
        long endTime = System.currentTimeMillis();
        Integer seconds = (int)((endTime-mStartTime)/1000);

        SoundWaves.getAppContext(this).getAnalystics().trackEvent(IAnalytics.EVENT_TYPE.INTRO_DURATION, seconds);
        super.onDestroy();
    }
}
