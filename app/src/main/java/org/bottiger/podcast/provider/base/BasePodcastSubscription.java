package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.request.target.PreloadTarget;

import org.bottiger.podcast.provider.PersistedSubscription;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.StrUtils;

/**
 * Created by aplb on 29-11-2016.
 */

public abstract class BasePodcastSubscription extends PersistedSubscription {

    public void fetchImage(@NonNull Context argContext) {
        PreloadTarget<Bitmap> preloadTarget = PreloadTarget.obtain(500, 500);
        String url = getImageURL();
        if (StrUtils.isValidUrl(url)) {
            ImageLoaderUtils.getGlide(argContext.getApplicationContext(), url)
                    .into(preloadTarget);
        }
    }

}
