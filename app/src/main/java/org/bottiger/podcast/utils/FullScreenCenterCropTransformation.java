package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Transformation;

import org.acra.ACRA;
import org.acra.log.ACRALog;

/**
 * Created by apl on 09-11-2014.
 */
public class FullScreenCenterCropTransformation implements Transformation {

    final int topVersusBottomCrop = 1; // 8 means that for each 1 pixel we crop from the top we crop 7 pixels from the bottom.

    private double m_viewWidth = -1;
    private int m_viewHeight;
    final Bitmap.Config m_bitmapConfig;
    final Bitmap.CompressFormat m_bitmapFormat;

    final String m_key;

    public FullScreenCenterCropTransformation(@NonNull Context argContext) {

        if (m_viewWidth < 0 || m_viewHeight < 0) {
            // screen size
            WindowManager wm = (WindowManager) argContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            m_viewWidth = size.x;
            m_viewHeight = size.y;
        }

        // Bitmap config
        m_bitmapConfig = Bitmap.Config.RGB_565;

        // Format
        m_bitmapFormat = Bitmap.CompressFormat.WEBP;

        m_key = Integer.toString(39715767 ^ m_viewHeight);
    }

    public static FullScreenCenterCropTransformation getmImageTransformation(Context argContext, FullScreenCenterCropTransformation argBackgroundTransformation, int argHeight) {
        if (argBackgroundTransformation == null) {
            argBackgroundTransformation = new FullScreenCenterCropTransformation(argContext);
        }

        return argBackgroundTransformation;
    }

    public static FullScreenCenterCropTransformation getmImageTransformation(Context argContext, FullScreenCenterCropTransformation argBackgroundTransformation, ImageView argImageView) {
        if (argBackgroundTransformation == null) {
            argBackgroundTransformation = new FullScreenCenterCropTransformation(argContext);
        }

        return argBackgroundTransformation;
    }

    @Override
    public Bitmap transform(Bitmap source) {

        double sourceWidth = source.getWidth();
        double sourceHeight = source.getWidth();

        double xScaling = m_viewHeight / sourceHeight;
        int imageHeight = (int)m_viewHeight;

        // filter = true: http://stackoverflow.com/questions/2895065/what-does-the-filter-parameter-to-createscaledbitmap-do
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, imageHeight, imageHeight, true);

        // https://github.com/square/picasso/issues/489
        source.recycle();

        int cropStartY = 0;

        if (m_viewWidth < imageHeight) {
            int pixelsToCrop = (int)(sourceWidth - m_viewWidth);
            cropStartY = pixelsToCrop / topVersusBottomCrop;
        }

        Bitmap croppedBitmap = null;
        try {
            croppedBitmap = Bitmap.createBitmap(scaledBitmap, cropStartY, 0, (int) m_viewWidth, m_viewHeight);
        } catch (IllegalArgumentException iae) {
            Log.e("BitmapCreationError", "cropStartY: " + cropStartY + " w: " + m_viewWidth + " h: " + m_viewHeight);
            String keyValue = "cropStartY:" + Integer.toString(cropStartY) + " " +
                    "m_viewWidth:" + Double.toString(m_viewWidth) + " " +
                    "m_viewHeight:" + Double.toString(m_viewHeight) + " " +
                    "sourceWidth:" + Double.toString(sourceWidth) + " " +
                    "sourceHeight:" + Double.toString(sourceHeight) + " ";
            ACRA.getErrorReporter()
                    .putCustomData("TransformationBug", keyValue);
            ACRA.getErrorReporter().handleException(iae);
            return scaledBitmap;
        }
        scaledBitmap.recycle();

        // Consider converting the images to RGB_565 to save memory
        // http://stackoverflow.com/questions/15058905/converting-bitmap-in-memory-to-bitmap-with-bitmap-config-rgb-565
        Bitmap lowBitrate = convert(croppedBitmap, m_bitmapConfig);

        return lowBitrate;
    }

    @Override
    public String key() {
        return m_key;
    }

    // Convert to RGB_565
    private Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
        Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return convertedBitmap;
    }
}

