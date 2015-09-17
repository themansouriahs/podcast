package org.bottiger.podcast;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

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

    public void testSettingsNavigartion() {
        // Open the overflow menu OR open the options menu,
        // depending on if the device has a hardware or software overflow menu button.
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        //onView(withId(R.id.menu_settings)).perform(click());
        onView(withText(R.string.menu_settings)).perform(click());
    }
}
