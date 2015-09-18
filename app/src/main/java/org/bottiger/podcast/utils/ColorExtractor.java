package org.bottiger.podcast.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import org.bottiger.podcast.R;

/**
 * Created by apl on 15-03-2015.
 */
public class ColorExtractor {

    private int mPrimary       = -1;
    private int mPrimaryTint   = -1;
    private int mSecondary     = -1;
    private int mSecondaryTint = -1;

    private int mTextColor     = -1;

    private Context mContext;

    public ColorExtractor(@Nullable Palette argPalette) {
        init(argPalette);
    }

    public ColorExtractor(@NonNull Context argContext, @Nullable Palette argPalette) {
        mContext = argContext;
        init(argPalette);
    }

    private void init(@Nullable Palette argPalette) {
        loadPrimaryColor(argPalette);
        loadPrimaryTintColor(argPalette);
        loadSecondaryColor(argPalette);
        loadSecondaryTintColor(argPalette);
        loadTextColor(argPalette);
    }

    public int getPrimary() {
        return mPrimary;
    }

    public int getPrimaryTint() {
        return mPrimaryTint;
    }

    public int getSecondary() {
        return mSecondary;
    }

    public int getSecondaryTint() {
        return mSecondaryTint;
    }

    public int getTextColor() {
        return mTextColor;
    }

    private void loadTextColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getDarkVibrantSwatch() : null;
        if (swatch == null) {
            if (mContext == null)
                return;

            mTextColor = mContext.getResources().getColor(R.color.white_opaque);
            return;
        }
        mTextColor = swatch.getBodyTextColor();
    }

    private void loadPrimaryColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getDarkVibrantSwatch() : null;
        mPrimary = getColor(swatch, R.color.colorPrimary);
    }

    private void loadPrimaryTintColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getLightVibrantSwatch() : null;
        mPrimaryTint = getColor(swatch, R.color.colorPrimaryDark);
    }

    private void loadSecondaryColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getMutedSwatch() : null;
        mSecondary = getColor(swatch, R.color.colorSecondary);
    }

    private void loadSecondaryTintColor(@Nullable Palette argPalette) {
        Palette.Swatch swatch = argPalette != null ? argPalette.getLightMutedSwatch() : null;
        mSecondaryTint = getColor(swatch, R.color.colorAccent);
    }

    private int getColor(@Nullable Palette.Swatch argSwatch, int defaultColorResource) {
        if (argSwatch == null) {
            if (mContext == null)
                return -1;

            return mContext.getResources().getColor(defaultColorResource);
        }

        return argSwatch.getRgb();
    }
}
