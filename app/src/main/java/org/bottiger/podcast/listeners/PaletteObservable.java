package org.bottiger.podcast.listeners;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.Log;

import org.bottiger.podcast.R;

import java.util.HashSet;
import java.util.WeakHashMap;

/**
 * Created by apl on 05-08-2014.
 */
public class PaletteObservable {

    private static WeakHashMap<PaletteListener, Boolean> mListeners = new WeakHashMap<PaletteListener, Boolean>();

    public static void registerListener(PaletteListener listener) {
        mListeners.put(listener, true);
    }

    public static boolean unregisterListener(PaletteListener listener) {
        try {
            return mListeners.remove(listener);
        } catch (Exception e) {
            //Log.d(e.printStackTrace();)
            e.printStackTrace(); // FIXME: This should not happen
            return false;
        }
    }

    public static void clear() {
        mListeners.clear();
    }

    public static void updatePalette(@NonNull String argUrl, @NonNull Palette argPalette) {
        if (argPalette == null) {
            return;
        }

        for (PaletteListener item : mListeners.keySet()) {

            if (item.getPaletteUrl().equals(argUrl)) {

                item.onPaletteFound(argPalette);
            }
        }
    }
}
