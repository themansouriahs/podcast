package org.bottiger.podcast;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import org.bottiger.podcast.TestUtils.RecyclerTestUtils;
import org.bottiger.podcast.TestUtils.TestUtils;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

/**
 * Created by aplb on 17-09-2015.
 */
public class EspressoPlaylistTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public EspressoPlaylistTest() {
        super(MainActivity.class);
    }

    public EspressoPlaylistTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
        TestUtils.unsubscribeAll(getActivity());
    }

    public void testQueueManually() {
        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        onView(withId(R.id.radioNone)).check(matches(isChecked()));
        TestUtils.subscribe(1);
        TestUtils.ViewPagerMove(TestUtils.LEFT);

        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));
    }

    public void testRecentEpisodes() {
        /*
        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        onView(withId(R.id.radioAll)).perform(click());
        onView(withId(R.id.radioAll)).check(matches(isChecked()));
        TestUtils.subscribe(1);
        TestUtils.ViewPagerMove(TestUtils.LEFT);

        onView(withId(R.id.playlist_empty_header)).check(matches(not(isDisplayed())));
        */
    }
}
