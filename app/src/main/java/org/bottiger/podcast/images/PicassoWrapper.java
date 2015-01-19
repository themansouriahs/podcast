package org.bottiger.podcast.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.FullScreenCenterCropTransformation;
import org.bottiger.podcast.views.RelativeLayoutWithBackground;
import org.bottiger.podcast.views.SquareImageView;

/**
 * Created by apl on 11-03-14.
 *
 * Provides a simple wrapper for Picasso.
 * The wrapper calls Picasso with the correct parameters and perform some simple
 * object caching.
 */
public class PicassoWrapper {

    private static final boolean debugging = false;
    private static Context mContext;
    private static Picasso mPicasso;

    private static Picasso init(Context context, boolean skipCache) {
        if (context == null) {
            throw new IllegalArgumentException();
        }

        Picasso picasso = null;

        if (skipCache) {
            picasso = newInstance(context);
        }
        else {
            picasso = mPicasso;
            if (context != mContext) {
                mContext = context;
                mPicasso = newInstance(context);
                picasso = mPicasso;
            }
        }

        picasso.setLoggingEnabled(false);
        picasso.setIndicatorsEnabled(false); // debugging

        return picasso;
    }

    private static Picasso newInstance(Context argContext) {
        LruCache cache = new LruCache(argContext);
        //LruCache cacheLarge = new LruCache(cache.maxSize()*3);
        return new Picasso.Builder(argContext).memoryCache(cache).build();
    }

    public static void load(Context context, String image, ImageView imageView, Transformation transformation, Callback argCallback)
    {
        load(context, image, imageView, transformation, argCallback, false);
    }

    public static void load(Context context, String image, ImageView imageView, Transformation transformation, Callback argCallback, boolean skipCache)
    {
        Picasso picasso = PicassoWrapper.init(context, skipCache);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        //picasso.load(image).resize(width,width).centerCrop().config(Bitmap.Config.RGB_565)

        if (argCallback != null) {
            picasso.load(image).transform(transformation)
                    .into(imageView, argCallback); // .fit().centerCrop()
        } else {
            picasso.load(image).transform(transformation)
                    .into(imageView);
        }
    }

    public static void simpleLoad(Context mContext, String logo, SquareImageView image) {
        Picasso picasso = PicassoWrapper.init(mContext, false);
        picasso.load(logo).fit().centerCrop().into(image);
    }

    public static void simpleLoad(Context mContext, String image, Target argTarget) {
        Picasso picasso = PicassoWrapper.init(mContext, false);
        picasso.load(image).transform(new FullScreenCenterCropTransformation(mContext)).into(argTarget);
    }

    public static void fetch(@NonNull Context argContext, @NonNull String argImageUrl, Transformation transformation) {
        Picasso picasso = PicassoWrapper.init(argContext, false);

        RequestCreator request = picasso.load(argImageUrl);
        if (transformation != null) {
            request.transform(transformation);
        }

        request.fetch();
    }
}
