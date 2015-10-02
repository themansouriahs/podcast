package org.bottiger.podcast.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 30-09-2015.
 */
public class ImageLoaderUtils {

    private static Context sContext;

    public static void loadImageInto(@NonNull ImageView argImageView, @Nullable String argUrl, boolean argUsePlaceholder) {
        if (argUrl == null)
            return;

        loadImageUsingGlide(argImageView, argUrl, argUsePlaceholder);
    }

    private static void loadImageUsingGlide(@NonNull ImageView argImageView, @Nullable String argUrl, boolean argUsePlaceholder) {
        sContext = argImageView.getContext();

        if (argUsePlaceholder) {
            Glide.with(sContext).load(argUrl).centerCrop().placeholder(R.drawable.generic_podcast).into(argImageView);
        } else {
            Glide.with(sContext).load(argUrl).centerCrop().into(argImageView);
        }
    }

}
