package org.bottiger.podcast.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.widget.Button;
import android.widget.ImageView;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 30-09-2015.
 */
public class ColorUtils {

    public static @ColorInt int getTextColor(@NonNull Context argContext) {
        return getColor(argContext, R.attr.themeTextColorPrimary);
    }

    public static @ColorInt int getFadedTextColor(@NonNull Context argContext) {
        return getColor(argContext, R.attr.themeTextColorFaded);
    }

    public static @ColorInt int getBackgroundColor(@NonNull Context argContext) {
        return getColor(argContext, R.attr.themeBackground);
    }

    public static @ColorInt int getIconColor(@NonNull Context argContext) {
        return getColor(argContext, android.R.attr.textColorPrimary);
    }

    public static void tintButton(@NonNull Button argButton, @NonNull ColorExtractor argColorExtractor) {
        tintButton(argButton, argColorExtractor.getPrimary());
    }

    public static void tintButton(@NonNull ImageView argButton, @NonNull ColorExtractor argColorExtractor) {
        if (Build.VERSION.SDK_INT >= 21) {
            argButton.setImageTintList(argColorExtractor.getColorStateList());
        } else {
            tintButton(argButton, argColorExtractor.getPrimary());
        }
    }

    public static @ColorInt int adjustToTheme(@NonNull Resources argResources, @Nullable Palette argPalette, @ColorInt int color) {
        int currentNightMode = argResources.getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        if (argPalette == null)
            return color;

        if (UIUtils.isInNightMode(argResources)) {
            return darken(argPalette.getDarkVibrantColor(color), 0.5f);
        } else {
            return argPalette.getLightVibrantColor(color);
        }
    }

    public static @ColorInt int adjustToThemeDark(@NonNull Resources argResources, @Nullable Palette argPalette, @ColorInt int color) {
        int currentNightMode = argResources.getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        if (argPalette == null)
            return color;

        if (UIUtils.isInNightMode(argResources)) {
            return darken(argPalette.getDarkVibrantColor(color), 0.5f);
        } else {
            return argPalette.getDarkVibrantColor(color);
        }
    }

    public static @ColorInt int darken(@ColorInt int argColor, float scale) {
        float[] hsv = new float[3];
        Color.colorToHSV(argColor, hsv);
        hsv[2] *= scale; // value component
        argColor = Color.HSVToColor(hsv);

        return argColor;
    }

    public static void tintButton(@NonNull Button argButton, @NonNull @ColorInt int argColor) {
        argButton.setTextColor(argColor);
    }

    public static void tintButton(@NonNull ImageView argButton, @NonNull @ColorInt int argColor) {
        argButton.setColorFilter(argColor);
    }

    private static @ColorInt int getColor(@NonNull Context argContext, @AttrRes int argResource) {
        ThemeHelper helper = new ThemeHelper(argContext);
        int colorRes = helper.getAttr(argResource);
        int color = ContextCompat.getColor(argContext, colorRes);
        return color;
    }

}
