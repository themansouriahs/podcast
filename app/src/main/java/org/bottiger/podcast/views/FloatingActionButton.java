package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.utils.ColorExtractor;

/**
 * Created by apl on 12-02-2015.
 */
public class FloatingActionButton extends com.melnykov.fab.FloatingActionButton implements PaletteListener {

    private String mURL;
    private Context mContext;

    public FloatingActionButton(Context context) {
        super(context);
        mContext = context;
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setPaletteUrl(String argURL) {
        mURL = argURL;
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {

        /*
        Palette.Swatch s = argChangedPalette.getVibrantSwatch();
        int color = PlayerButtonView.StaticButtonColor(argChangedPalette);
        int colorPressed = argChangedPalette.getLightMutedSwatch().getRgb();
        //int colorRipple = argChangedPalette.getLightVibrantSwatch().getRgb();
        int colorRipple = s == null ? color : s.getRgb();

        super.setColorNormal(color);
        super.setColorPressed(colorPressed);
        super.setColorRipple(colorRipple);
        */
        ColorExtractor extractor = new ColorExtractor(mContext, argChangedPalette);
        super.setColorNormal(extractor.getPrimary());
        super.setColorPressed(extractor.getSecondary());
        super.setColorRipple(extractor.getPrimaryTint());

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
