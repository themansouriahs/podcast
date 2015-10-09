package org.bottiger.podcast.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.LruCache;
import android.util.Patterns;
import android.webkit.URLUtil;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.bottiger.podcast.listeners.PaletteListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by apl on 10-11-2014.
 */
public class PaletteHelper {

    private static final int CACHE_SIZE = 20;
    private static final int PALETTE_SIZE = 24; /* 24 is default size. You can decrease this value to speed up palette generation */

    private static final ReentrantLock sLock = new ReentrantLock();

    private static LruCache<String, Palette> mPaletteCache = new LruCache<>(CACHE_SIZE);
    private static HashMap<String, HashSet<PaletteListener>> mWaiting = new HashMap<>();

    public static synchronized void generate(@NonNull final String argUrl, @NonNull final Activity argActivity, @Nullable final PaletteListener ... argCallbacks) {

        if (!Patterns.WEB_URL.matcher(argUrl).matches())
            return;

        try {
            sLock.lock();

            Palette palette = mPaletteCache.get(argUrl);
            if (palette != null) {
                for (PaletteListener callback : argCallbacks)
                    callback.onPaletteFound(palette);
                return;
            }

            boolean isWaiting = mWaiting.containsKey(argUrl);

            addToWaiingLine(argUrl, argCallbacks);

            if (isWaiting)
                return;

        } finally {
            sLock.unlock();
        }


        Glide.with(argActivity)
                .load(argUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(100,100) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        // You can use the bitmap in only limited ways
                        // No need to do any cleanup.
                        //mLock.lock();
                        final Palette palette = Palette.generate(resource, PALETTE_SIZE);

                        sLock.lock();
                        try {

                            if (palette != null) {
                                mPaletteCache.put(argUrl, palette);
                            }

                            final HashSet<PaletteListener> listeners = mWaiting.get(argUrl);

                            if (listeners == null)
                                return;

                            for (final PaletteListener listener : listeners) {

                                argActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onPaletteFound(palette);
                                    }
                                });

                            }

                            mWaiting.put(argUrl, new HashSet<PaletteListener>());
                        } finally {
                            sLock.unlock();
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        sLock.lock();
                        try {
                            mWaiting.remove(argUrl);
                        } finally {
                            sLock.unlock();
                        }
                    }
                });
    }

    private static void addToWaiingLine(@NonNull String argUrl, @Nullable PaletteListener ... argNewCallbacks) {
        HashSet<PaletteListener> listeners = mWaiting.get(argUrl);

        if (listeners == null) {
            listeners = new HashSet<>();
        }

        if (argNewCallbacks == null)
            return;

        for (PaletteListener callback : argNewCallbacks) {
            if (listeners.contains(callback))
                continue;

            listeners.add(callback);
        }

        mWaiting.put(argUrl, listeners);
    }
}
