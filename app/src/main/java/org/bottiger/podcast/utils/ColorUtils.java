package org.bottiger.podcast.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import org.bottiger.podcast.provider.ISubscription;

/**
 * Created by aplb on 30-09-2015.
 */
public class ColorUtils {

    @Nullable private static ColorDrawable sBlackColorDrawable = null;

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

    public static @ColorInt int adjustToTheme(@NonNull Resources argResources, @ColorInt int argColor) {
        if (UIUtils.isInNightMode(argResources) || isLight(argColor)) {
            return darken(argColor, 0.5f);
        } else {
            return argColor;
        }
    }

    public static @ColorInt int adjustToTheme(@NonNull Resources argResources, @NonNull ISubscription argSubscription) {
        return adjustToTheme(argResources, argSubscription.getPrimaryColor());
    }

    public static @ColorInt int adjustToTheme(@NonNull Resources argResources, @Nullable Palette argPalette, @ColorInt int color) {
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

    public static void tintButton(@NonNull Button argButton, @NonNull @ColorInt int argColor) {
        argButton.setTextColor(argColor);
    }

    public static void tintButton(@NonNull ImageView argButton, @NonNull @ColorInt int argColor) {
        argButton.setColorFilter(argColor);
    }

    public static @ColorInt int darken(@ColorInt int argColor, float scale) {
        float[] hsv = new float[3];
        Color.colorToHSV(argColor, hsv);
        hsv[2] *= scale; // value component
        argColor = Color.HSVToColor(hsv);

        return argColor;
    }

    // https://en.wikipedia.org/wiki/HSL_and_HSV#/media/File:Hsl-hsv_models.svg
    public static @ColorInt int lighten(@ColorInt int argColor) {
        float threshold = 0.5f;

        float[] hsl = new float[3];
        android.support.v4.graphics.ColorUtils.colorToHSL(argColor, hsl);

        float light = hsl[2];
        if (light > threshold) {
            light = light - 0.1f;
        } else {
            light = 0.8f; //light + 0.45f;
        }

        hsl[2] = light;
        int color = android.support.v4.graphics.ColorUtils.HSLToColor(hsl);

        return color;
    }

    // https://en.wikipedia.org/wiki/HSL_and_HSV#/media/File:Hsl-hsv_models.svg
    public static boolean isLight(@ColorInt int argColor) {
        float threshold = 0.8f;

        float[] hsl = new float[3];
        android.support.v4.graphics.ColorUtils.colorToHSL(argColor, hsl);

        return hsl[2] > threshold;
    }

    public static ColorDrawable getSubscriptionBackgroundColor(@NonNull Resources argResources, @NonNull ISubscription argSubscription) {
        if (UIUtils.isInNightMode(argResources)) {
            if (sBlackColorDrawable == null) {
                sBlackColorDrawable =  new ColorDrawable(Color.BLACK);
            }

            return sBlackColorDrawable;
        }

        @ColorInt int background = argSubscription.getPrimaryColor();
        return new ColorDrawable(lighten(background));
    }

    private static @ColorInt int getColor(@NonNull Context argContext, @AttrRes int argResource) {
        ThemeHelper helper = new ThemeHelper(argContext);
        int colorRes = helper.getAttr(argResource);
        int color = ContextCompat.getColor(argContext, colorRes);
        return color;
    }

}
