package org.bottiger.podcast;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.test.ActivityInstrumentationTestCase2;

import org.bottiger.podcast.TestUtils.RecyclerTestUtils;
import org.bottiger.podcast.TestUtils.TestUtils;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by aplb on 17-09-2015.
 */
public class FeedViewTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final int FIRST_ITEM = 0;

    public FeedViewTest() {
        super(MainActivity.class);
    }

    public FeedViewTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        getActivity();
        TestUtils.clearAllData(getActivity());
        TestUtils.subscribe(1);
        //RecyclerTestUtils.withRecyclerView(R.id.gridview).atPosition(0).perform(click());
        onView(withId(R.id.gridview))
                .perform(RecyclerTestUtils.actionOnItemViewAtPosition(FIRST_ITEM, R.id.grid_title, click()));
    }

    public void testView() {
        onView(withId(R.id.multiscroller)).check(matches(isDisplayed()));
    }
    public void testFeedViewSettings() {
        onView(withId(R.id.feedview_fap_button)).perform(click());

        onView(withId(R.id.feed_activity_settings_container)).check(matches(isDisplayed()));
    }

    public void testRecyclerviewContent() {
        onView(withId(R.id.feed_recycler_view)).check(matches(isDisplayed()));

        onView(withId(R.id.feed_subscribe_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        RecyclerTestUtils.withRecyclerView(R.id.feed_recycler_view)
                .atPosition(FIRST_ITEM)
                .matches(isDisplayed());
    }

}
