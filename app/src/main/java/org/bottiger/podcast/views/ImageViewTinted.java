package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.bottiger.podcast.utils.ColorExtractor;

/**
 * Created by apl on 21-03-2015.
 */
public class ImageViewTinted extends ImageView {

    String mPaletteKey = null;

    public ImageViewTinted(Context context) {
        super(context);
    }

    public ImageViewTinted(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewTinted(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(argChangedPalette);
        //setColorFilter(extractor.getSecondary(), PorterDuff.Mode.LIGHTEN   );
        //setColorFilter(Color.DKGRAY, PorterDuff.Mode.SCREEN);
        //setColorFilter(Color.DKGRAY, PorterDuff.Mode.DST);
        invalidate();
    }
}
