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

    private void loadPrimaryColor(@Nullable Palette argPalette) {
        mPrimary = getColor(argPalette.getVibrantSwatch(), R.color.colorPrimary);
    }

    private void loadPrimaryTintColor(@Nullable Palette argPalette) {
        mPrimaryTint = getColor(argPalette.getDarkVibrantSwatch(), R.color.colorPrimaryDark);
    }

    private void loadSecondaryColor(@Nullable Palette argPalette) {
        mSecondary = getColor(argPalette.getMutedSwatch(), R.color.colorSecondary);
    }

    private void loadSecondaryTintColor(@Nullable Palette argPalette) {
        mSecondary = getColor(argPalette.getLightMutedSwatch(), R.color.colorAccent);
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
