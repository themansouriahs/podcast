package org.bottiger.podcast.listeners;

import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by apl on 05-08-2014.
 */
public class PaletteObservable {

    private static final ReentrantLock sLock = new ReentrantLock();
    private static HashMap<PaletteListener, Boolean> mListeners = new HashMap<PaletteListener, Boolean>();

    public static void registerListener(PaletteListener listener) {
        sLock.lock();
        try {
            mListeners.put(listener, true);
        } finally {
            sLock.unlock();
        }
    }

    public static boolean unregisterListener(PaletteListener listener) {
        sLock.lock();
        try {
            synchronized (mListeners) {
                if (!mListeners.containsKey(listener))
                    return false;

                return mListeners.remove(listener);
            }
        } catch (Exception e) {
            //Log.d(e.printStackTrace();)
            e.printStackTrace(); // FIXME: This should not happen
            return false;
        } finally {
            sLock.unlock();
        }
    }

    public static void clear() {
        sLock.lock();
        try {
            mListeners.clear();
        } finally {
            sLock.unlock();
        }
    }

    public static void updatePalette(@NonNull String argUrl, @NonNull Palette argPalette) {
        if (argPalette == null) {
            return;
        }

        sLock.lock();
        try {

            for (PaletteListener item : mListeners.keySet()) {

                if (item.getPaletteUrl().equals(argUrl)) {
                    item.onPaletteFound(argPalette);
                }
            }

        } catch (NullPointerException npe) {

            return; // FIXME
        } finally {
            sLock.unlock();
        }
    }
}
