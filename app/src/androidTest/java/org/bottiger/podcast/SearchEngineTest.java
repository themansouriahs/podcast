package org.bottiger.podcast;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
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
public class SearchEngineTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void searchEngineTest() {

        ViewInteraction appCompatTextView5 = onView(
                allOf(withText("DISCOVER"), isDisplayed()));
        appCompatTextView5.perform(click());

        ViewInteraction searchAutoComplete = onView(
                allOf(withId(R.id.search_src_text),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));

        searchAutoComplete.perform(replaceText("harry potter"), closeSoftKeyboard());

        ViewInteraction textView = onView(
                allOf(withId(R.id.result_title), withText("MuggleCast: the Harry Potter podcast"),
                        childAtPosition(
                                allOf(withId(R.id.result_container),
                                        childAtPosition(
                                                withId(R.id.container),
                                                1)),
                                0),
                        isDisplayed()));
        textView.check(matches(isDisplayed()));

        ViewInteraction appCompatImageView = onView(
                allOf(withId(R.id.search_close_btn), withContentDescription("Clear query"),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        appCompatImageView.perform(click());

        ViewInteraction appCompatImageButton5 = onView(
                allOf(withId(R.id.discovery_searchIcon), withContentDescription("Podcast directory (search engine)"),
                        withParent(allOf(withId(R.id.discovery_searchView_container_relative),
                                withParent(withId(R.id.discovery_searchView_container)))),
                        isDisplayed()));
        appCompatImageButton5.perform(click());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(android.R.id.text1), withText("gPodder"),
                        childAtPosition(
                                allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                                        withParent(withClassName(is("android.widget.LinearLayout")))),
                                0),
                        isDisplayed()));
        appCompatTextView.perform(click());

        ViewInteraction searchAutoComplete2 = onView(
                allOf(withId(R.id.search_src_text),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        searchAutoComplete2.perform(replaceText("nerd"), closeSoftKeyboard());

        ViewInteraction searchAutoComplete3 = onView(
                allOf(withId(R.id.search_src_text), withText("nerd"),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        searchAutoComplete3.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.result_title), withText("Jovem Nerd"),
                        childAtPosition(
                                allOf(withId(R.id.result_container),
                                        childAtPosition(
                                                withId(R.id.container),
                                                1)),
                                0),
                        isDisplayed()));
        textView2.check(matches(isDisplayed()));

        ViewInteraction appCompatImageView2 = onView(
                allOf(withId(R.id.search_close_btn), withContentDescription("Clear query"),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        appCompatImageView2.perform(click());

        ViewInteraction appCompatImageButton6 = onView(
                allOf(withId(R.id.discovery_searchIcon), withContentDescription("Podcast directory (search engine)"),
                        withParent(allOf(withId(R.id.discovery_searchView_container_relative),
                                withParent(withId(R.id.discovery_searchView_container)))),
                        isDisplayed()));
        appCompatImageButton6.perform(click());

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(android.R.id.text1), withText("AudioSear.ch"),
                        childAtPosition(
                                allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                                        withParent(withClassName(is("android.widget.LinearLayout")))),
                                2),
                        isDisplayed()));
        appCompatTextView2.perform(click());

        ViewInteraction searchAutoComplete4 = onView(
                allOf(withId(R.id.search_src_text),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        searchAutoComplete4.perform(click());

        ViewInteraction searchAutoComplete5 = onView(
                allOf(withId(R.id.search_src_text),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        searchAutoComplete5.perform(replaceText("tech"), closeSoftKeyboard());

        ViewInteraction relativeLayout = onView(
                allOf(withId(R.id.container),
                        childAtPosition(
                                allOf(withId(R.id.search_result_view),
                                        childAtPosition(
                                                withId(R.id.discovery_container),
                                                1)),
                                0),
                        isDisplayed()));
        relativeLayout.check(matches(isDisplayed()));

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
