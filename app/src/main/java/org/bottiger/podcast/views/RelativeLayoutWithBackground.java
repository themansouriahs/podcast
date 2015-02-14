package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class RelativeLayoutWithBackground extends RelativeLayout implements Target {

    public RelativeLayoutWithBackground(Context context) {
        super(context);
    }

    public RelativeLayoutWithBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutWithBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RelativeLayoutWithBackground(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        BitmapDrawable background;
        background = new BitmapDrawable(getContext().getResources(), bitmap);
        background.setAlpha(50);
        setBackground(background);
    }

    @Override
    public void onBitmapFailed(Drawable argDrawable) {
        return;
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        return;
    }
}
