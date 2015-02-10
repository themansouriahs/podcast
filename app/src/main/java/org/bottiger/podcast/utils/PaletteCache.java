package org.bottiger.podcast.utils;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;

/**
 * Created by apl on 10-11-2014.
 */
public class PaletteCache {

    private static final int PALETTE_SIZE = 24; /* 24 is default size. You can decrease this value to speed up palette generation */
    public static final HashMap<String,Palette> mPaletteCache = new HashMap<String, Palette>();

    public static Palette get(@NonNull PaletteListener argPaletteListener) {
        return get(argPaletteListener.getPaletteUrl());
    }

    public static Palette get(@NonNull String argUrl) {
        return mPaletteCache.get(argUrl);
    }

    public static void generateAsync(@NonNull final String argUrl, @NonNull Bitmap argBitmap) {
        Palette.generateAsync(argBitmap, PALETTE_SIZE, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                put(argUrl, palette);
            }
        });
    }

    public static Palette generate(@NonNull String argUrl, @NonNull Bitmap argBitmap) {
        return generate(argUrl, argBitmap, false);
    }

    public static Palette generate(@NonNull String argUrl, @NonNull Bitmap argBitmap, Boolean forceUpdate) {
        Palette currentPalette = PaletteCache.get(argUrl);
        if (forceUpdate || currentPalette == null) {
            Palette palette = Palette.generate(argBitmap, PALETTE_SIZE);
            put(argUrl, palette);
            return palette;
        }

        return currentPalette;
    }

    public static void put(@NonNull String argUrl, @NonNull Palette argPalette) {
        mPaletteCache.put(argUrl, argPalette);
        PaletteObservable.updatePalette(argUrl, argPalette);
    }

    public static boolean containsKey(@NonNull String argUrl) {
        return mPaletteCache.containsKey(argUrl);
    }
}
