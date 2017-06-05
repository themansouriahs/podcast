package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.bottiger.podcast.R;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 30-09-2015.
 */
public class ImageLoaderUtils {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NETWORK, NO_NETWORK, DEFAULT})
    public @interface NetworkPolicy {}
    public static final int NETWORK = 1;
    public static final int NO_NETWORK = 2;
    public static final int DEFAULT = 3;

    public static final DiskCacheStrategy SW_DiskCacheStrategy = DiskCacheStrategy.ALL;

    public static RequestBuilder<Bitmap> getGlide(@NonNull Context argContext, @NonNull String argUrl) {
        return getGlide(argContext, argUrl, DEFAULT);
    }

    public static RequestBuilder<Bitmap> getGlide(@NonNull Context argContext, @NonNull String argUrl, @NetworkPolicy int argAllowNetwork) {
        RequestManager requestManager = Glide.with(argContext);

        RequestBuilder<Bitmap> bitmapRequestBuilder;
        boolean noNetwork = argAllowNetwork == NO_NETWORK || (argAllowNetwork != NETWORK && NetworkUtils.getNetworkStatus(argContext, false) != SoundWavesDownloadManager.NETWORK_OK);

        bitmapRequestBuilder = requestManager.asBitmap();

        noNetwork = false;
        if (noNetwork) {
            //bitmapRequestBuilder = bitmapRequestBuilder.using(new NetworkDisablingLoader()).load(argUrl);
        } else {
            bitmapRequestBuilder = bitmapRequestBuilder.load(argUrl);
        }

        //                 .diskCacheStrategy( SW_DiskCacheStrategy );

        return bitmapRequestBuilder;
    }

    @Deprecated
    public static void loadImageInto(@NonNull View argImageView,
                                     @Nullable String argUrl,
                                     boolean argUsePlaceholder,
                                     boolean argRoundedCorners,
                                     @NetworkPolicy int argNetworkPolicy) {
        loadImageUsingGlide(argImageView, argUrl, null, true, argUsePlaceholder, argRoundedCorners, argNetworkPolicy, null);
    }

    @Deprecated
    public static void loadImageInto(@NonNull View argImageView,
                                     @Nullable String argUrl,
                                     @NetworkPolicy int argNetworkPolicy,
                                     @NonNull RequestOptions options) {
        loadImageUsingGlide(argImageView, argUrl, null, true, false, false, argNetworkPolicy, options);
    }

    private static void loadImageUsingGlide(final @NonNull View argTargetView,
                                            @Nullable String argUrl,
                                            @Nullable Transformation argTransformation,
                                            boolean argDoCrop,
                                            boolean argUsePlaceholder,
                                            final boolean argRounddedCorners,
                                            @NetworkPolicy int argNetworkPolicy,
                                            @Nullable RequestOptions options) {
        Context context = argTargetView.getContext();

        Target target;

        if (argTargetView instanceof ImageView) {
            target = getImageViewTarget((ImageView)argTargetView, argRounddedCorners);
        } else {
            target = getViewTarget(argTargetView, argRounddedCorners);
        }

        RequestBuilder<Bitmap> builder = ImageLoaderUtils.getGlide(context, argUrl, argNetworkPolicy);

        if (options == null) {
            options = new RequestOptions();
            if (argDoCrop)
                options.centerCrop();
            else
                options.override(512, 512).fitCenter();

            if (argUsePlaceholder) {
                options.placeholder(R.drawable.generic_podcast);
            }
        }

        builder.apply(options);
        builder.into(target);
    }

    private static SimpleTarget<Bitmap> getViewTarget(@NonNull final View argImageView, final boolean argRounddedCorners) {
        return new SimpleTarget<Bitmap>(512, 512) {
            @Override
            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                BitmapDrawable bd = new BitmapDrawable(argImageView.getResources(), resource);
                argImageView.setBackground(bd);
            }
        };
    }

    private static BitmapImageViewTarget getImageViewTarget(@NonNull final ImageView argImageView, final boolean argRounddedCorners) {
        return new BitmapImageViewTarget(argImageView) {
            @Override
            protected void setResource(Bitmap resource) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(argImageView.getResources(), resource);
                float radius = argImageView.getResources().getDimension(R.dimen.playlist_image_radius_small);
                radius = argRounddedCorners ? radius : 0;
                circularBitmapDrawable.setCornerRadius(radius);

                argImageView.setImageDrawable(circularBitmapDrawable);
            }
        };
    }

}
