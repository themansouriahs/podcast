package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 30-09-2015.
 */
public class ImageLoaderUtils {

    private static Context sContext;

    public static void loadImageInto(@NonNull ImageView argImageView,
                                     @Nullable String argUrl,
                                     boolean argUsePlaceholder,
                                     boolean argRoundedCorners) {
        if (argUrl == null)
            return;

        loadImageUsingGlide(argImageView, argUrl, argUsePlaceholder, argRoundedCorners);
    }

    private static void loadImageUsingGlide(final @NonNull ImageView argImageView,
                                            @Nullable String argUrl,
                                            boolean argUsePlaceholder,
                                            final boolean argRounddedCorners) {
        sContext = argImageView.getContext();

        Target target  = new BitmapImageViewTarget(argImageView) {
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

        if (argUsePlaceholder) {
            Glide.with(sContext).load(argUrl).asBitmap().centerCrop().placeholder(R.drawable.generic_podcast).into(target);
        } else {
            Glide.with(sContext).load(argUrl).asBitmap().centerCrop().into(target);
        }
    }

    /*

                Glide.with(mActivity).load(image).asBitmap().centerCrop().into(new BitmapImageViewTarget(viewHolder.mPodcastImage) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(mActivity.getResources(), resource);
                    float radius = mActivity.getResources().getDimension(R.dimen.playlist_image_radius_small);
                    //circularBitmapDrawable.setCircular(true);
                    circularBitmapDrawable.setCornerRadius(radius);
                    viewHolder.mPodcastImage.setImageDrawable(circularBitmapDrawable);
                }
            });

     */

}
