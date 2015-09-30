package org.bottiger.podcast.utils;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 30-09-2015.
 */
public class ColorUtils {

    public static @ColorInt int getTextColor(@NonNull Context argContext) {
        return getColor(argContext, R.attr.themeTextColorPrimary);
    }

    public static @ColorInt int getFadedTextColor(@NonNull Context argContext) {
        return getColor(argContext, R.attr.themeTextColorPrimary);
    }

    public static @ColorInt int getBackgroundColor(@NonNull Context argContext) {
        return getColor(argContext, R.attr.themeBackground);
    }

    private static @ColorInt int getColor(@NonNull Context argContext, @AttrRes int argResource) {
        ThemeHelper helper = new ThemeHelper(argContext);
        int colorRes = helper.getAttr(argResource);
        int color = ContextCompat.getColor(argContext, colorRes);
        return color;
    }

}
