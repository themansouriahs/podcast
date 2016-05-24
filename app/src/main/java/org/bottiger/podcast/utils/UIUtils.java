package org.bottiger.podcast.utils;

/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.DrawerActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
/**
 * An assortment of UI helpers.
 */
public class UIUtils {
    private static final String TAG = "Google UIUtils";

    /**
     * Factor applied to session color to derive the background color on panels and when
     * a session photo could not be downloaded (or while it is being downloaded)
     */
    public static final float SESSION_BG_COLOR_SCALE_FACTOR = 0.65f;
    public static final float SESSION_PHOTO_SCRIM_ALPHA = 0.75f;


    public static final String TARGET_FORM_FACTOR_ACTIVITY_METADATA =
            "com.google.samples.apps.iosched.meta.TARGET_FORM_FACTOR";

    public static final String TARGET_FORM_FACTOR_HANDSET = "handset";
    public static final String TARGET_FORM_FACTOR_TABLET = "tablet";

    /**
     * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    /**
     * Regex to search for HTML escape sequences.
     *
     * <p></p>Searches for any continuous string of characters starting with an ampersand and ending with a
     * semicolon. (Example: &amp;amp;)
     */
    private static final Pattern REGEX_HTML_ESCAPE = Pattern.compile(".*&\\S;.*");

    private static CharSequence sNowPlayingText;
    private static CharSequence sLivestreamNowText;
    private static CharSequence sLivestreamAvailableText;

    public static final String GOOGLE_PLUS_PACKAGE_NAME = "com.google.android.apps.plus";
    public static final String YOUTUBE_PACKAGE_NAME = "com.google.android.youtube";

    public static final int ANIMATION_FADE_IN_TIME = 250;
    public static final String TRACK_ICONS_TAG = "tracks";

    private static SimpleDateFormat sDayOfWeekFormat = new SimpleDateFormat("E");
    private static DateFormat sShortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);


    /**
     * Populate the given {@link android.widget.TextView} with the requested text, formatting
     * through {@link android.text.Html#fromHtml(String)} when applicable. Also sets
     * {@link android.widget.TextView#setMovementMethod} so inline links are handled.
     */
    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if ((text.contains("<") && text.contains(">")) || REGEX_HTML_ESCAPE.matcher(text).find()) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);

        // Walk through string, inserting bold snippet spans
        int startIndex, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);

            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);

            // Insert bold style
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //builder.setSpan(new ForegroundColorSpan(0xff111111),
            //        startIndex - delta, endIndex - delta - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            delta += 2;
        }

        return builder;
    }

    public static void disPlayBottomSnackBar(View argView, @StringRes int stringRes, View.OnClickListener argAction, boolean addBototmPadding) {
        disPlayBottomSnackBarInternalInternal(argView, getSnack(argView, stringRes), argAction, addBototmPadding);
    }

    public static void disPlayBottomSnackBar(View argView, CharSequence stringRes, View.OnClickListener argAction, boolean addBototmPadding) {
        disPlayBottomSnackBarInternalInternal(argView, getSnack(argView, stringRes), argAction, addBototmPadding);
    }

    private static void disPlayBottomSnackBarInternalInternal(View argView, Snackbar argSnack, View.OnClickListener argAction, boolean addBototmPadding) {
        if (argAction != null) {
            argSnack = argSnack.setAction(R.string.playlist_episode_dismissed_undo, argAction);
        }

        argSnack.setActionTextColor(ContextCompat.getColor(argView.getContext(), R.color.white_opaque));

        if (addBototmPadding) {
            View view = argSnack.getView();
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
            params.bottomMargin = DrawerActivity.getStatusBarHeight(view.getContext().getResources())*2;
            view.setLayoutParams(params);
        }

        argSnack.show();
    }

    private static Snackbar getSnack(View argView, Object stringRes) {
        if (stringRes instanceof CharSequence) {
            return Snackbar.make(argView, (CharSequence) stringRes, Snackbar.LENGTH_LONG);
        }

        return Snackbar.make(argView, (int)stringRes, Snackbar.LENGTH_LONG);
    }

    public static void preferPackageForIntent(Context context, Intent intent, String packageName) {
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo resolveInfo : pm.queryIntentActivities(intent, 0)) {
            if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                intent.setPackage(packageName);
                break;
            }
        }
    }

    public static int getScreenHeight(@NonNull Context argContext) {
        WindowManager windowManager = (WindowManager) argContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        return height;
    }

    @TargetApi(19)
    public static Transition getDefaultTransition(@NonNull Resources argResources) {
        AutoTransition autoTransition = new AutoTransition();
        autoTransition.setDuration(argResources.getInteger(R.integer.animation_quick));
        autoTransition.setOrdering(TransitionSet.ORDERING_TOGETHER);
        return autoTransition;
    }

    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener){
        if (Build.VERSION.SDK_INT < 16) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    private static final int BRIGHTNESS_THRESHOLD = 130;

    /**
     * Calculate whether a color is light or dark, based on a commonly known
     * brightness formula.
     *
     * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
     */
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    // Whether a feedback notification was fired for a particular session. In the event that a
    // feedback notification has not been fired yet, return false and set the bit.
    public static boolean isFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    // Clear the flag that says a notification was fired for the given session.
    // Typically used to debug notifications.
    public static void unmarkFeedbackNotificationFiredForSession(Context context, String sessionId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("feedback_notification_fired_%s", sessionId);
        sp.edit().putBoolean(key, false).commit();
    }
    // Shows whether a notification was fired for a particular session time block. In the
    // event that notification has not been fired yet, return false and set the bit.
    public static boolean isNotificationFiredForBlock(Context context, String blockId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = String.format("notification_fired_%s", blockId);
        boolean fired = sp.getBoolean(key, false);
        sp.edit().putBoolean(key, true).commit();
        return fired;
    }

    private static final long sAppLoadTime = System.currentTimeMillis();

    public static long getCurrentTime(final Context context) {
        if (BuildConfig.DEBUG) {
            return context.getSharedPreferences("mock_data", Context.MODE_PRIVATE)
                    .getLong("mock_current_time", System.currentTimeMillis())
                    + System.currentTimeMillis() - sAppLoadTime;
//            return ParserUtils.parseTime("2012-06-27T09:44:45.000-07:00")
//                    + System.currentTimeMillis() - sAppLoadTime;
        } else {
            return System.currentTimeMillis();
        }
    }

    private static final int[] RES_IDS_ACTION_BAR_SIZE = { android.R.attr.actionBarSize };

    /** Calculates the Action Bar height in pixels. */
    public static int calculateActionBarSize(Context context) {
        if (context == null) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }

        float size = att.getDimension(0, 0);
        att.recycle();
        return (int) size;
    }

    public static int setColorAlpha(int color, float alpha) {
        int alpha_int = Math.min(Math.max((int) (alpha * 255.0f), 0), 255);
        return Color.argb(alpha_int, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int scaleColor(int color, float factor, boolean scaleAlpha) {
        return Color.argb(scaleAlpha ? (Math.round(Color.alpha(color) * factor)) : Color.alpha(color),
                Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    public static int scaleSessionColorToDefaultBG(int color) {
        return scaleColor(color, SESSION_BG_COLOR_SCALE_FACTOR, false);
    }

    public static boolean hasActionBar(Activity activity) {
        return activity.getActionBar() != null;
    }

    public static void setStartPadding(final Context context, View view, int padding) {
        if (isRtl(context)) {
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), padding, view.getPaddingBottom());
        } else {
            view.setPadding(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else {
            return context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        }
    }

    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public static float getProgress(int value, int min, int max) {
        if (min == max) {
            throw new IllegalArgumentException("Max (" + max + ") cannot equal min (" + min + ")");
        }

        return (value - min) / (float) (max - min);
    }



    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public static int NavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int orientation = resources.getConfiguration().orientation;

        int id = resources.getIdentifier(
                orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape",
                "dimen", "android");
        if (id > 0) {
            return resources.getDimensionPixelSize(id);
        }
        return 0;
    }

    public static void tintStatusBar(int argColor, Activity argActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = argActivity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(argColor);
        }
    }

    public static void resetStatusBar(@NonNull Activity argActivity) {
        int color = attrColor(R.attr.colorPrimaryDark, argActivity); //ContextCompat.getColor(argActivity, R.attr.colorPrimaryDark);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int opaqueColor = Color.argb(255, r, g, b);
        tintStatusBar(opaqueColor, argActivity);
    }

    public static @ColorInt  int attrColor(@AttrRes int argRes, @NonNull Context argContext) {
        TypedValue typedValue = new TypedValue();

        // R.attr.colorAccent
        TypedArray a = argContext.obtainStyledAttributes(typedValue.data, new int[] { argRes });
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
    }

    public static boolean isInNightMode() {

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
            return true;

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO)
            return false;

        int currentNightMode = SoundWaves.getAppContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // We don't know what mode we're in, assume notnight
                return false;
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're in day time
                return false;
            case Configuration.UI_MODE_NIGHT_YES: {
                // Night mode is active, we're at night!
                //color = (int) (color * 0.2);
                //color = darken(color, 1f);
                return true;
            }
        }

        return false;
    }

    /**
     <string-array name="pref_theme_entries">
     <item>Light (default)</item>
     <item>Dark</item>
     <item>Auto (Day/Night)</item>
     </string-array>

     <string-array name="pref_theme_entries_values" translatable="false">
     <item>1</item>
     <item>2</item>
     <item>3</item>
     </string-array>

     *
     *
      * @param argAppCompatActivity
     */
    public static void setTheme(@NonNull Context argAppCompatActivity) {
        String theme = PreferenceHelper.getStringPreferenceValue(argAppCompatActivity,
                R.string.pref_dark_theme_key,
                R.string.pref_theme_default);

        int themeIntval = Integer.parseInt(theme);

        if (themeIntval == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (themeIntval == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        if (themeIntval == 3) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

}

