package org.bottiger.podcast.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.LruCache;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.utils.rxbus.RxBasicSubscriber;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by apl on 10-11-2014.
 */
public class PaletteHelper {

    private static final int CACHE_SIZE = 40;
    private static final int PALETTE_SIZE = 24; /* 24 is default size. You can decrease this value to speed up palette generation */

    private static LruCache<String, Palette> mPaletteCache = new LruCache<>(CACHE_SIZE);

    private static ReentrantLock sLock = new ReentrantLock();
    private static HashMap<String, List<PaletteListener>> listeners = new HashMap<>();

    public static void generate(@NonNull final org.bottiger.podcast.provider.ISubscription argSubscription, @NonNull final Activity argActivity, @Nullable final PaletteListener ... argCallbacks) {

        final String url = argSubscription.getImageURL();

        if (!StrUtils.isValidUrl(url))
            return;

        if (argCallbacks == null)
            return;

        Palette palette = mPaletteCache.get(url);

        if (palette != null && argCallbacks != null) {
            for (PaletteListener callback : argCallbacks)
                callback.onPaletteFound(palette);
            return;
        }

        final String key = url;
        try {
            sLock.lock();
            boolean hasKey = listeners.containsKey(key);
            List<PaletteListener> list = hasKey ? listeners.get(key) : new LinkedList<PaletteListener>();
            Collections.addAll(list, argCallbacks);
            listeners.put(key, list);

            if (hasKey) {
                return;
            }

        } finally {
            sLock.unlock();
        }

        ImageLoaderUtils.getGlide(argActivity, url)
                .into(new SimpleTarget<Bitmap>(200, 200) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {

                        Flowable.just(resource)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.computation())
                                .map(new Function<Bitmap, Palette>( ) {
                                    @Override
                                    public Palette apply(Bitmap bitmap) throws Exception {
                                        Palette palette = Palette.from(bitmap).generate();

                                        if (palette != null) {
                                            mPaletteCache.put(url, palette);
                                        }

                                        argSubscription.onPaletteFound(palette);

                                        return palette;
                                    }
                                }).subscribe(new RxBasicSubscriber<Palette>() {
                            @Override
                            public void onNext(Palette palette) {

                                List<PaletteListener> list = listeners.get(key);
                                for (final PaletteListener listener : list) {
                                    listener.onPaletteFound(palette);
                                }
                            }
                        });
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    }
                });
    }
}
