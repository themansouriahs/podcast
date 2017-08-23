package org.bottiger.podcast.images;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.module.GlideModule;

/**
 * Created by aplb on 30-09-2015.
 */
public class SoundWavesGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Apply options to the builder here.
        super.applyOptions(context, builder);
    }
}