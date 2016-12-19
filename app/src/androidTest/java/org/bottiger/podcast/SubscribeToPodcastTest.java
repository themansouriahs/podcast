package org.bottiger.podcast;


import android.app.Activity;
import android.content.Context;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.SwitchCompat;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.jakewharton.espresso.OkHttp3IdlingResource;

import org.bottiger.podcast.TestUtils.TestUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.OkHttpClient;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SubscribeToPodcastTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void unsubscribeFromAll() {
        Activity activity = mActivityTestRule.getActivity();
        TestUtils.unsubscribeAll(activity);
        TestUtils.clearDatabase(activity);
        TestUtils.firstRun(activity, false);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void subscribe() {
        ViewInteraction appCompatTextView4 = onView(
                allOf(withText("DISCOVER"), isDisplayed()));
        appCompatTextView4.perform(click());

        ViewInteraction switchCompat = onView(withId(R.id.search_result_view))
                .perform(RecyclerViewActions
                        .actionOnItemAtPosition(2, new ViewAction() {
                            @Override
                            public Matcher<View> getConstraints() {
                                return null;
                            }

                            @Override
                            public String getDescription() {
                                return "Click the subscribe button";
                            }

                            @Override
                            public void perform(UiController uiController, View view) {
                                SwitchCompat button = (SwitchCompat) view.findViewById(R.id.result_subscribe_switch);
                                // Maybe check for null
                                button.performClick();
                            }
                        }));

        switchCompat.perform(click());

        ViewInteraction appCompatTextView5 = onView(
                allOf(withText("SUBSCRIPTIONS"), isDisplayed()));
        appCompatTextView5.perform(click());

        Context context = mActivityTestRule.getActivity().getApplicationContext();
        OkHttpClient client = SoundWaves.getAppContext(context).getRefreshManager().getHttpClient();
        IdlingResource resource = OkHttp3IdlingResource.create("OkHttp", client);

        Espresso.registerIdlingResources(resource);

        // Open the subscription
        onView(withId(R.id.gridview)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Scroll down
        onView(withId(R.id.feed_recycler_view))
                .check(matches(isDisplayed()))
                .perform(RecyclerViewActions.scrollToPosition(10));

    }

}
