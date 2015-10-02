package org.bottiger.podcast;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.text.TextUtils;

import org.bottiger.podcast.TestUtils.RecyclerTestUtils;
import org.bottiger.podcast.TestUtils.TestUtils;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.PlayerService;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;


/**
 * Created by apl on 20-05-2015.
 */
public class EspressoBasicViewPagerTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity mActivity;

    public EspressoBasicViewPagerTest() {
        super(MainActivity.class);
    }

    public EspressoBasicViewPagerTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {

        mActivity = getActivity();
        cleanData();

        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }

    @Before
    public void cleanData() {
        TestUtils.clearAllData(mActivity);
    }

    // https://github.com/googlesamples/android-testing/blob/master/downloads/espresso-cheat-sheet-2.1.0.pdf
    public void testWelcomeScreen() {

        TestUtils.ViewPagerMove(TestUtils.LEFT);

        // Validate initial state
        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        onView(withId(R.id.radioNone)).check(matches(isDisplayed()));
        onView(withId(R.id.radioAll)).check(matches(isDisplayed()));

        onView(withId(R.id.radioNone)).check(matches(isChecked()));
        onView(withId(R.id.radioAll)).check(matches(isNotChecked()));

        // Validate button click
        onView(withId(R.id.radioAll)).perform(click());

        onView(withId(R.id.radioNone)).check(matches(isNotChecked()));
        onView(withId(R.id.radioAll)).check(matches(isChecked()));
    }

    /**
     * Based on https://code.google.com/p/android-test-kit/source/browse/espresso/libtests/src/main/java/com/google/android/apps/common/testing/ui/espresso/action/SwipeActionIntegrationTest.java?r=c4e4da01ca8d0fab31129c87f525f6e9ba1ecc02
     */
    public void testSwipeToSubscriptionFragment() {

        TestUtils.ViewPagerMove(TestUtils.LEFT);

        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        onView(withId(R.id.app_content))
                .check(matches(hasDescendant(withId(R.id.playlist_empty_header))));

        onView(withId(R.id.playlist_welcome_screen))
                .perform(swipeLeft());

        onView(withId(R.id.app_content))
                .check(matches(hasDescendant(withId(R.id.subscription_empty))));

        onView(withId(R.id.app_content))
                .perform(swipeLeft());

        onView(withId(R.id.app_content))
                .check(matches(hasDescendant(withId(R.id.discovery_search_container))));

        // Go back to start
        onView(withId(R.id.app_content))
                .perform(swipeRight())
                .perform(swipeRight());

        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));
    }


    public void testSubscribing() {

        TestUtils.clearDatabase(mActivity);

        TestUtils.subscribe(4);
    }

}
