package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.bottiger.podcast.BuildConfig;
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
