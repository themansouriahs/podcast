package org.bottiger.podcast;


import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.bottiger.podcast.TestUtils.TestUtils;
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
import static org.bottiger.podcast.TestUtils.RecyclerTestUtils.withRecyclerView;
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

        ViewInteraction appCompatImageButton7 = onView(
                allOf(withId(R.id.discovery_searchIcon), withContentDescription("Podcast directory (search engine)"),
                        withParent(allOf(withId(R.id.discovery_searchView_container_relative),
                                withParent(withId(R.id.discovery_searchView_container)))),
                        isDisplayed()));
        appCompatImageButton7.perform(click());

        String itunes = mActivityTestRule
                .getActivity()
                .getResources()
                .getString(R.string.webservices_discovery_engine_itunes);
        ViewInteraction appCompatTextView4 = onView(allOf(withId(android.R.id.text1),
                withText(itunes)))
                .check(matches(isDisplayed()));
        appCompatTextView4.perform(click());


        ViewInteraction searchAutoComplete = onView(
                allOf(withId(R.id.search_src_text),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));

        searchAutoComplete.perform(replaceText("harry potter"), closeSoftKeyboard());

        waitForSearchResult(mActivityTestRule.getActivity());

        onView(withRecyclerView(R.id.search_result_view)
                .atPositionOnView(0, R.id.result_title))
                .check(matches(withText("MuggleCast: the Harry Potter podcast")));

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

        String gpodder = mActivityTestRule
                .getActivity()
                .getResources()
                .getString(R.string.webservices_discovery_engine_gpodder);
        ViewInteraction appCompatTextView = onView(allOf(withId(android.R.id.text1),
                withText(gpodder)))
                .check(matches(isDisplayed()));

        appCompatTextView.perform(click());

        ViewInteraction searchAutoComplete2 = onView(
                allOf(withId(R.id.search_src_text),
                        withParent(allOf(withId(R.id.search_plate),
                                withParent(withId(R.id.search_edit_frame)))),
                        isDisplayed()));
        searchAutoComplete2.perform(replaceText("nerd"), closeSoftKeyboard());

        waitForSearchResult(mActivityTestRule.getActivity());

        onView(withRecyclerView(R.id.search_result_view)
                .atPositionOnView(0, R.id.result_title))
                .check(matches(withText("Jovem Nerd")));

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

        String audiosearch = mActivityTestRule
                .getActivity()
                .getResources()
                .getString(R.string.webservices_discovery_engine_audiosearch);
        ViewInteraction appCompatTextView2 = onView(allOf(withId(android.R.id.text1),
                withText(audiosearch)))
                .check(matches(isDisplayed()));

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
        searchAutoComplete5.perform(replaceText("Android"), closeSoftKeyboard());

        waitForSearchResult(mActivityTestRule.getActivity());

        onView(withRecyclerView(R.id.search_result_view)
                .atPositionOnView(0, R.id.result_title))
                .check(matches(withText("Android Police")));

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

    private static void waitForSearchResult(@NonNull FragmentContainerActivity argActivity) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FragmentContainerActivity.SectionsPagerAdapter sectionsPagerAdapter = argActivity.getSectionsPagerAdapter();
        DiscoveryFragment discoveryFragment = (DiscoveryFragment) sectionsPagerAdapter.getItem(2);
        TestUtils.waitForOkHttp(discoveryFragment.getOkHttpClient());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
