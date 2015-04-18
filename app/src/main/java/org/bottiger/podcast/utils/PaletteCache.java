package org.bottiger.podcast.utils;

import android.content.Context;
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
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;

/**
 * Created by apl on 10-11-2014.
 */
public class PaletteCache {

    private static final int CACHE_SIZE = 100;
    private static final int PALETTE_SIZE = 24; /* 24 is default size. You can decrease this value to speed up palette generation */
    
    private static LruCache<String, Palette> mPaletteCache = new LruCache<>(CACHE_SIZE);

    public static Palette get(@NonNull PaletteListener argPaletteListener) {
        return get(argPaletteListener.getPaletteUrl());
    }

    public static Palette get(@NonNull String argUrl) {
        return mPaletteCache.get(argUrl);
    }

    public static void getAsync(@NonNull final String argUrl, @NonNull Bitmap argBitmap) {
        Palette.generateAsync(argBitmap, PALETTE_SIZE, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                put(argUrl, palette);
            }
        });
    }

    public static void generate(@NonNull final String argUrl, @NonNull Context argContext) {

        final Uri url;
        try {
            url = Uri.parse(argUrl);
            if (url == null) {
                return;
            }
        } catch (NullPointerException npe) {
            return;
        }

        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(url)
                .build();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>>
                dataSource = imagePipeline.fetchDecodedImage(request, argContext);


        DirectExecutor directExecutor = new DirectExecutor();

        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                // You can use the bitmap in only limited ways
                // No need to do any cleanup.
                //mLock.lock();
                    Palette palette = PaletteCache.get(argUrl);
                    if (palette == null) {
                        PaletteCache.generate(argUrl, bitmap, false);
                    }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                // No cleanup required here.
            }
        }, directExecutor);
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
        return mPaletteCache.get(argUrl) != null;
        //return mPaletteCache.containsKey(argUrl);
    }

    public static void generatePalletFromUrl(@Nullable String argUrl) {

    }
}
