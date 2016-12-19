package org.bottiger.podcast;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.bottiger.podcast.TestUtils.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class IntroActivityComplete {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setAsFirstRun() {
        // Specify a valid string.
        Context context = (mActivityTestRule.getActivity());
        TestUtils.firstRun(context, true);
    }

    @Test
    public void introActivityComplete() {
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.mi_button_next), withContentDescription("Next"), isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withId(R.id.mi_button_next), withContentDescription("Next"), isDisplayed()));
        appCompatImageButton2.perform(click());

        ViewInteraction appCompatImageButton3 = onView(
                allOf(withId(R.id.mi_button_next), withContentDescription("Next"), isDisplayed()));
        appCompatImageButton3.perform(click());

        ViewInteraction appCompatImageButton4 = onView(
                allOf(withId(R.id.mi_button_next), withContentDescription("Next"), isDisplayed()));
        appCompatImageButton4.perform(click());

    }

}
