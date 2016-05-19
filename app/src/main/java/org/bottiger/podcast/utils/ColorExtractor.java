package org.bottiger.podcast.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;

/**
 * Created by apl on 15-03-2015.
 */
public class ColorExtractor {

    private @ColorInt int mBaseColor      = -1;

    private @ColorInt int mPrimary       = -1;
    private @ColorInt int mPrimaryTint   = -1;
    private @ColorInt int mSecondary     = -1;
    private @ColorInt int mSecondaryTint = -1;

    private @ColorInt int mTextColor     = -1;

    private ColorStateList mColorStateList;

    @Nullable
    private Context mContext;

    public ColorExtractor(@Nullable Palette argPalette) {
        init(argPalette, -1);
    }

    public ColorExtractor(@Nullable Palette argPalette, @ColorInt int argBaseColor) {
        init(argPalette, argBaseColor);
    }

    public ColorExtractor(@NonNull Context argContext, @Nullable Palette argPalette) {
        mContext = argContext;
        init(argPalette, -1);
    }

    public ColorExtractor(@NonNull Context argContext, @Nullable Palette argPalette, @ColorInt int argBaseColor) {
        mContext = argContext;
        init(argPalette, argBaseColor);
    }

    private void init(@Nullable Palette argPalette, @ColorInt int argBaseColor) {
        mBaseColor = argBaseColor;

        mPrimary = mBaseColor;
        mPrimaryTint = mBaseColor;
        mSecondary = mBaseColor;
        mSecondaryTint = mBaseColor;

        loadPrimaryColor(argPalette);
        loadPrimaryTintColor(argPalette);
        loadSecondaryColor(argPalette);
        loadSecondaryTintColor(argPalette);
        loadTextColor(argPalette);
        setColorStateList();
    }

    @ColorInt
    public int getPrimary() {
        return mPrimary;
    }

    @ColorInt
    public int getPrimaryTint() {
        return mPrimaryTint;
    }

    @ColorInt
    public int getSecondary() {
        return mSecondary;
    }

    @ColorInt
    public int getSecondaryTint() {
        return mSecondaryTint;
    }

    @ColorInt
    public int getTextColor() {
        return mTextColor;
    }

    @Nullable
    public ColorStateList getColorStateList() {
        return mColorStateList;
    }

    private void setColorStateList() {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
                new int[] {-android.R.attr.state_checked}, // unchecked
                new int[] { android.R.attr.state_pressed}  // pressed
        };

        // Remember to update tintButton() if you change this
        int[] colors = new int[] {
                getSecondaryTint(),
                getSecondaryTint(),
                getSecondaryTint(),
                getSecondaryTint()
        };

        mColorStateList = new ColorStateList(states, colors);
    }

    private void loadTextColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getDarkVibrantSwatch() : null;
        if (swatch == null) {
            if (mContext == null)
                return;

            mTextColor = ContextCompat.getColor(mContext, R.color.pitch_black);
            return;
        }
        mTextColor = swatch.getBodyTextColor();
    }

    private void loadPrimaryColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getDarkVibrantSwatch() : null; // was dark
        mPrimary = getColor(swatch, R.color.colorPrimary);
    }

    private void loadPrimaryTintColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getLightMutedSwatch() : null; // was light
        mPrimaryTint = getColor(swatch, R.color.colorPrimaryDark);
    }

    private void loadSecondaryColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getDarkMutedSwatch() : null;
        mSecondary = getColor(swatch, R.color.colorSecondary);
    }

    private void loadSecondaryTintColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getLightVibrantSwatch() : null;
        mSecondaryTint = getColor(swatch, R.color.colorAccent);
    }

    private int getColor(@Nullable Palette.Swatch argSwatch, int defaultColorResource) {

        int color = -1;

        if (argSwatch == null) {
            if (mContext == null) {
                color = mBaseColor;
            } else {
                color = ContextCompat.getColor(mContext, defaultColorResource);
            }
        } else {
            color = argSwatch.getRgb();
        }

        /*
            int currentNightMode = SoundWaves.getAppContext().getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;

            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    // We don't know what mode we're in, assume notnight
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're in day time
                    return color;
                case Configuration.UI_MODE_NIGHT_YES: {
                    // Night mode is active, we're at night!
                    //color = (int) (color * 0.2);

                    float[] hsv = new float[3];
                    Color.colorToHSV(color, hsv);
                    hsv[2] *= 0.2f; // value component
                    color = Color.HSVToColor(hsv);
                }
            }
            */

        return color;
    }
}
