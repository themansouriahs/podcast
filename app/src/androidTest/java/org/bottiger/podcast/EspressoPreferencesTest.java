package org.bottiger.podcast;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import org.bottiger.podcast.TestUtils.TestUtils;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * Created by aplb on 17-09-2015.
 */
public class EspressoPreferencesTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public EspressoPreferencesTest() {
        super(MainActivity.class);
    }

    public EspressoPreferencesTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }

    public void testEmptyPlaylistButtons() {
        TestUtils.ViewPagerMove(TestUtils.RIGHT);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String analyticsKey = getKey(R.string.pref_anonymous_feedback_key);

        /*
        boolean initialValue = prefs.getBoolean(analyticsKey, !BuildConfig.PRIVATE_MODE);

        if (initialValue) {
            onView(withId(R.id.checkBox_usage)).check(matches(isChecked()));
            onView(withId(R.id.checkBox_usage)).perform(click());
            onView(withId(R.id.checkBox_usage)).check(matches(not(isChecked())));
        } else {
            onView(withId(R.id.checkBox_usage)).check(matches(not(isChecked())));
            onView(withId(R.id.checkBox_usage)).perform(click());
            onView(withId(R.id.checkBox_usage)).check(matches(isChecked()));
        }

        boolean finalValue = prefs.getBoolean(analyticsKey, !BuildConfig.PRIVATE_MODE);
        assertTrue(initialValue != finalValue);
        */
        /*
        String couldKey = getKey(R.string.pref_cloud_support_key);
        boolean initialValue2 = prefs.getBoolean(couldKey, !BuildConfig.PRIVATE_MODE);


        if (initialValue2) {
            onView(withId(R.id.checkBox_cloud)).check(matches(isChecked()));
            onView(withId(R.id.checkBox_cloud)).perform(click());
            onView(withId(R.id.checkBox_cloud)).check(matches(not(isChecked())));
        } else {
            onView(withId(R.id.checkBox_cloud)).check(matches(not(isChecked())));
            onView(withId(R.id.checkBox_cloud)).perform(click());
            onView(withId(R.id.checkBox_cloud)).check(matches(isChecked()));
        }
        */
        /*
        if (initialValue2) {
            onView(withId(R.id.checkBox_cloud))
                    .check(matches(isChecked()))
                    .perform(click())
                    .check(matches(not(isChecked())));
        } else {
            onView(withId(R.id.checkBox_cloud))
                    .check(matches(not(isChecked())))
                    .perform(click())
                    .check(matches(isChecked()));
        }*/
        /*
        boolean finalValue2 = prefs.getBoolean(couldKey, !BuildConfig.PRIVATE_MODE);
        assertTrue(initialValue2 != finalValue2);
        */
    }

    public void testSettingsNavigartion() {
        // Open the overflow menu OR open the options menu,
        // depending on if the device has a hardware or software overflow menu button.
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        //onView(withId(R.id.menu_settings)).perform(click());
        onView(withText(R.string.menu_settings)).perform(click());
    }

    private String getKey(@StringRes int argId) {
        return getActivity().getResources().getString(argId);
    }
}
