package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Transformation;

/**
 * Created by apl on 29-07-2014.
 */
public class BackgroundTransformation implements Transformation {

    final int topVersusBottomCrop = 9; // 8 means that for each 1 pixel we crop from the top we crop 7 pixels from the bottom.

    private double m_imageWidth = -1;
    private int mScreeenWidth = -1;

    private int m_viewHeight;
    final Bitmap.Config m_bitmapConfig;
    final Bitmap.CompressFormat m_bitmapFormat;

    final String m_key;

    public BackgroundTransformation(@NonNull Context argContext, @NonNull int argViewHeightInPixels) {

        m_viewHeight = argViewHeightInPixels;

        if (m_imageWidth < 0) {
            // screen size
            WindowManager wm = (WindowManager) argContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            m_imageWidth = size.x;
            mScreeenWidth = size.x;
        }

        int maxSize = (int) Math.max(m_imageWidth, m_viewHeight);
        m_imageWidth = maxSize;
        m_viewHeight = maxSize;

        // Bitmap config
        m_bitmapConfig = Bitmap.Config.RGB_565;

        // Format
        m_bitmapFormat = Bitmap.CompressFormat.WEBP;

        m_key = Integer.toString(39715767 ^ m_viewHeight);
    }

    public static BackgroundTransformation getmImageTransformation(Context argContext, BackgroundTransformation argBackgroundTransformation, int argHeight) {
        if (argBackgroundTransformation == null) {
            argBackgroundTransformation = new BackgroundTransformation(argContext, argHeight);
        }

        return argBackgroundTransformation;
    }

    public static BackgroundTransformation getmImageTransformation(Context argContext, BackgroundTransformation argBackgroundTransformation, ImageView argImageView) {
        if (argBackgroundTransformation == null) {
            argBackgroundTransformation = new BackgroundTransformation(argContext, argImageView.getLayoutParams().height);
        }

        return argBackgroundTransformation;
    }

    @Override
    public Bitmap transform(Bitmap source) {

        double sourceWidth = source.getWidth();
        double sourceHeight = source.getWidth();

        double xScaling = m_imageWidth / sourceWidth;
        int imageHeight = (int)m_imageWidth;

        // filter = true: http://stackoverflow.com/questions/2895065/what-does-the-filter-parameter-to-createscaledbitmap-do
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, (int) m_imageWidth, imageHeight, true);

        // https://github.com/square/picasso/issues/489
        source.recycle();

        int cropStartY = 0;

        if (m_viewHeight < imageHeight) {
            double fractionShown = m_viewHeight / m_imageWidth;
            int pixelsToCrop = (int) (imageHeight - (imageHeight * fractionShown));
            cropStartY = pixelsToCrop / topVersusBottomCrop;
        }

        Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, 0, cropStartY, (int) m_imageWidth, m_viewHeight);
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
