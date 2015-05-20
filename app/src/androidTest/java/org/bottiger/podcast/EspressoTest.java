package org.bottiger.podcast;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.Before;

/**
 * Created by apl on 20-05-2015.
 */
public class EspressoTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity mActivity;

    public EspressoTest() {
        super("org.bottiger.podcast", MainActivity.class);
    }

    public EspressoTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();
    }

    public void testChangeText_sameActivity() {
        // Type text and then press the button.
        int j = 5;
        j = j+j;
        /*
        onView(withId(R.id.editTextUserInput))
                .perform(typeText(STRING_TO_BE_TYPED), closeSoftKeyboard());
        onView(withId(R.id.changeTextButton)).perform(click());

        // Check that the text was changed.
        ...
        */
    }

}
