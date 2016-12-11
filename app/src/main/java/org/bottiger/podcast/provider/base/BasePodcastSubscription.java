package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.PreloadTarget;

import org.bottiger.podcast.provider.PersistedSubscription;
import org.bottiger.podcast.utils.ImageLoaderUtils;

/**
 * Created by aplb on 29-11-2016.
 */

public abstract class BasePodcastSubscription extends PersistedSubscription {

    public void fetchImage(@NonNull Context argContext) {
        PreloadTarget<Bitmap> preloadTarget = PreloadTarget.obtain(500, 500);
        ImageLoaderUtils.getGlide(argContext.getApplicationContext(), getImageURL())
                .into(preloadTarget);
    }

}
