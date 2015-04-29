package org.bottiger.podcast.images;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import org.apache.commons.validator.routines.UrlValidator;

/**
 * Created by apl on 16-04-2015.
 */
public class FrescoHelper {

    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;

    private static UrlValidator validator = new UrlValidator();

    public static boolean validUrl(@Nullable String argUrl) {
        return validator.isValid(argUrl);
    }

    public static ImageRequest getImageRequest(@NonNull String argUrl, @Nullable Postprocessor argPostprocessor) {
        if (!validUrl(argUrl)) {
            throw new IllegalStateException("make sure the URL is valid");
        }

        Uri uri = Uri.parse(argUrl);

        Postprocessor processor = argPostprocessor != null ? argPostprocessor : new Defaultprocessor();

        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(new ResizeOptions(MAX_WIDTH, MAX_HEIGHT))
                .setPostprocessor(processor)
                .build();

        return request;
    }

    public static void loadImageInto(@NonNull SimpleDraweeView argView, @NonNull String argUrl, @Nullable Postprocessor argPostprocessor) {
        if (argView == null)
            return;

        if (!validUrl(argUrl)) {
            return;
        }

        ImageRequest request = getImageRequest(argUrl, argPostprocessor);

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setOldController(argView.getController())
                .build();
        argView.setController(controller);
        //Uri uri = Uri.parse(argUrl);
        //argView.setImageURI(uri);
    }

    public static CloseableReference<Bitmap> toCloseableReference(Bitmap argBitmap) {
        return CloseableReference.of(argBitmap, new ResourceReleaser<Bitmap>() {
            @Override
            public void release(Bitmap value) {
                value.recycle();
            }
        });
    }

    private static class Defaultprocessor extends BasePostprocessor {

        @Override
        public void process(Bitmap bitmap) {
            return;
        }

        @Override
        public String getName() {
            return "SoundWavesDefaultPostprocessor";
        }
    }

    public static class PalettePostProcessor extends BasePostprocessor {

        private Activity mActivity;
        private String url;

        public PalettePostProcessor(@NonNull Activity argActivity, @NonNull String argUrl) {
            mActivity = argActivity;
            url = argUrl;
        }

        @Override
        public void process(Bitmap bitmap) {
            // we can not copy the bitmap inside here
            //analyzeWhitenessOfPhotoAsynchronously(bitmap);

            //String url = item.image;
            //mLock.lock();

            if (bitmap.isRecycled())
                return;

            return;
        }

        @Override
        public String getName() {
            return "PalettePostProcessor";
        }
    }

    public static Bitmap copySmallBitmap(@NonNull Bitmap argBitmap, @NonNull PlatformBitmapFactory argPlatformBitmapFactory) {
        CloseableReference<Bitmap> bitmapRef = argPlatformBitmapFactory.createBitmap(
                argBitmap.getWidth() / 2,
                argBitmap.getHeight() / 2);
        try {
            Bitmap destBitmap = bitmapRef.get();
            for (int x = 0; x < destBitmap.getWidth(); x+=2) {
                for (int y = 0; y < destBitmap.getHeight(); y+=2) {
                    int color = argBitmap.getPixel(x, y);
                    destBitmap.setPixel(x, y, color);
                }
            }
            return CloseableReference.cloneOrNull(bitmapRef).get();
        } finally {
            CloseableReference.closeSafely(bitmapRef);
        }
    }
}
