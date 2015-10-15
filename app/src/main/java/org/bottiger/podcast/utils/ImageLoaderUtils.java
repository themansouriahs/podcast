package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

import org.bottiger.podcast.R;

import jp.wasabeef.glide.transformations.ColorFilterTransformation;

/**
 * Created by aplb on 30-09-2015.
 */
public class ImageLoaderUtils {

    private static Context sContext;

    @Deprecated
    public static void loadImageInto(@NonNull View argImageView,
                                     @Nullable String argUrl,
                                     boolean argUsePlaceholder,
                                     boolean argRoundedCorners) {
        loadImageUsingGlide(argImageView, argUrl, null, argUsePlaceholder, argRoundedCorners);
    }

    public static void loadImageInto(@NonNull View argImageView,
                                     @Nullable String argUrl,
                                     @Nullable Transformation argTransformation,
                                     boolean argUsePlaceholder,
                                     boolean argRoundedCorners) {
        if (argUrl == null)
            return;

        loadImageUsingGlide(argImageView, argUrl, argTransformation, argUsePlaceholder, argRoundedCorners);
    }

    private static void loadImageUsingGlide(final @NonNull View argTargetView,
                                            @Nullable String argUrl,
                                            @Nullable Transformation argTransformation,
                                            boolean argUsePlaceholder,
                                            final boolean argRounddedCorners) {
        sContext = argTargetView.getContext();

        Target target;

        if (argTargetView instanceof ImageView) {
            target = getImageViewTarget((ImageView)argTargetView, argRounddedCorners);
        } else {
            target = getViewTarget(argTargetView, argRounddedCorners);
        }

        DrawableTypeRequest request = Glide.with(sContext).load(argUrl);

        if (argTransformation != null) {
            int c = Color.argb(50, 0,0,0);
            Transformation t = new ColorFilterTransformation(argTargetView.getContext(), c);
            request.bitmapTransform(argTransformation, t).into((ImageView)argTargetView);
            return;
        }

        BitmapRequestBuilder builder = request.asBitmap().centerCrop();

        if (argUsePlaceholder) {
            builder.placeholder(R.drawable.generic_podcast);
        }

        builder.into(target);
    }

    private static SimpleTarget<Bitmap> getViewTarget(@NonNull final View argImageView, final boolean argRounddedCorners) {
        return new SimpleTarget<Bitmap>(512, 512) {
            @Override
            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                BitmapDrawable bd = new BitmapDrawable(argImageView.getResources(), bitmap);
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    argImageView.setBackgroundDrawable(bd);
                } else {
                    argImageView.setBackground(bd);
                }
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
