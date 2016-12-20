package org.bottiger.podcast;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.bottiger.podcast.TestUtils.RecyclerTestUtils;
import org.bottiger.podcast.TestUtils.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by aplb on 17-09-2015.
 */
@RunWith(AndroidJUnit4.class)
public class EspressoFeedViewTest {

    private static final int FIRST_ITEM = 0;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() {
        Activity activity = mActivityTestRule.getActivity();

        TestUtils.clearAllData(activity);
        TestUtils.subscribe(1);
        //RecyclerTestUtils.withRecyclerView(R.id.gridview).atPosition(0).perform(click());
        onView(withId(R.id.gridview))
                .perform(RecyclerTestUtils.actionOnItemViewAtPosition(FIRST_ITEM, R.id.grid_title, click()));
    }

    @Test
    public void testView() {
        TestUtils.waitForSubscriptionRefresh(mActivityTestRule.getActivity());

        onView(withId(R.id.multiscroller)).check(matches(isDisplayed()));
    }

    @Test
    public void testFeedViewSettings() {

        TestUtils.waitForSubscriptionRefresh(mActivityTestRule.getActivity());

        onView(withId(R.id.feedview_fap_button)).perform(click());
        onView(withId(R.id.feed_activity_settings_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerviewContent() {

        TestUtils.waitForSubscriptionRefresh(mActivityTestRule.getActivity());

        onView(withId(R.id.feed_recycler_view)).check(matches(isDisplayed()));

        //onView(withId(R.id.feed_subscribe_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        RecyclerTestUtils.withRecyclerView(R.id.feed_recycler_view)
                .atPosition(FIRST_ITEM)
                .matches(isDisplayed());
    }

}
