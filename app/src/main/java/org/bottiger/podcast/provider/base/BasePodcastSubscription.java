package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.PreloadTarget;

import org.bottiger.podcast.provider.PersistedSubscription;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.StrUtils;

/**
 * Created by aplb on 29-11-2016.
 */

public abstract class BasePodcastSubscription extends PersistedSubscription {

    public void fetchImage(@NonNull Context argContext) {
        String url = getImageURL();
        if (StrUtils.isValidUrl(url)) {
            RequestManager rm = Glide.with(argContext.getApplicationContext()); //ImageLoaderUtils.getGlide(argContext.getApplicationContext(), url);
            PreloadTarget<Bitmap> preloadTarget = PreloadTarget.obtain(rm, 500, 500);
            rm.asBitmap().load(url).into(preloadTarget);
        }
    }

}
