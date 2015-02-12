package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;

/**
 * Created by apl on 12-02-2015.
 */
public class FloatingActionButton extends com.melnykov.fab.FloatingActionButton implements PaletteListener {

    private String mURL;

    public FloatingActionButton(Context context) {
        super(context);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPaletteUrl(String argURL) {
        mURL = argURL;
        PaletteObservable.registerListener(this);
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        int color = PlayerButtonView.StaticButtonColor(argChangedPalette);
        int colorPressed = argChangedPalette.getLightMutedSwatch().getRgb();
        int colorRipple = argChangedPalette.getLightVibrantSwatch().getRgb();

        //setBackgroundColor(color);
        super.setColorNormal(color);
        super.setColorPressed(colorPressed);
        //super.setColorFilter(colorPressed);
        super.setColorRipple(colorRipple);

        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        if (!TextUtils.isEmpty(mURL)) {
            throw new IllegalStateException("URL must not be null");
        }

        return mURL;
    }
}
