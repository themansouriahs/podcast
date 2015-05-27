package org.bottiger.podcast.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.LruCache;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

import org.bottiger.podcast.images.FrescoHelper;
import org.bottiger.podcast.listeners.PaletteListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

        if (!FrescoHelper.validUrl(argUrl))
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

        ImageRequest request = FrescoHelper.getImageRequest(argUrl, null);

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>>
                dataSource = imagePipeline.fetchDecodedImage(request, argActivity);


        DirectExecutor directExecutor = new DirectExecutor();

        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                // You can use the bitmap in only limited ways
                // No need to do any cleanup.
                //mLock.lock();
                final Palette palette = Palette.generate(bitmap, PALETTE_SIZE);

                try {
                    sLock.lock();

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
            public void onFailureImpl(DataSource dataSource) {
                try {
                    sLock.lock();
                    mWaiting.remove(argUrl);
                } finally {
                    sLock.unlock();
                }
            }
        }, directExecutor);
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
