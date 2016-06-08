package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.PreloadTarget;
import com.bumptech.glide.request.target.SimpleTarget;

import org.bottiger.podcast.provider.ISubscription;

/**
 * Created by aplb on 08-06-2016.
 */

public abstract class BaseSubscription implements ISubscription {

    public void fetchImage(@NonNull Context argContext) {
        PreloadTarget<Bitmap> preloadTarget = PreloadTarget.obtain(500, 500);
        Glide.with(argContext.getApplicationContext())
        .load(getImageURL())
                .asBitmap()
                .into(preloadTarget);
    }
}
