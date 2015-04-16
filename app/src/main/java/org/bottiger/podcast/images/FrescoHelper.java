package org.bottiger.podcast.images;

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
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import org.apache.commons.validator.routines.UrlValidator;
import org.bottiger.podcast.utils.PaletteCache;

/**
 * Created by apl on 16-04-2015.
 */
public class FrescoHelper {

    private static UrlValidator validator = new UrlValidator();

    public static boolean validUrl(@Nullable String argUrl) {
        return validator.isValid(argUrl);
    }

    public static ImageRequest getImageRequest(@NonNull String argUrl, @Nullable Postprocessor argPostprocessor) {
        if (!validUrl(argUrl)) {
            throw new IllegalStateException("make sure the URL is valid");
        }

        Uri uri = Uri.parse(argUrl);

        Defaultprocessor processor = new Defaultprocessor(argUrl, argPostprocessor);

        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                //.setPostprocessor(processor)
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
    }

    private static class Defaultprocessor implements Postprocessor {

        private String mUrl;
        private Postprocessor mpostprocessor;

        public Defaultprocessor(@NonNull String argUrl, @Nullable Postprocessor argPostprocessor) {
            mUrl = argUrl;
            mpostprocessor = argPostprocessor;
        }

        @Override
        public CloseableReference<Bitmap> process(Bitmap bitmap, PlatformBitmapFactory platformBitmapFactory) {
            //PaletteCache.generate(mUrl, bitmap);
            if (mpostprocessor != null) {
                mpostprocessor.process(bitmap, platformBitmapFactory);
            }

            return toCloseableReference(bitmap);
        }

        @Override
        public String getName() {
            return "SoundWavesDefaultPostprocessor";
        }
    }

    public static CloseableReference<Bitmap> toCloseableReference(Bitmap argBitmap) {
        return CloseableReference.of(argBitmap, new ResourceReleaser<Bitmap>() {
            @Override
            public void release(Bitmap value) {
                value.recycle();
            }
        });
    }
}
