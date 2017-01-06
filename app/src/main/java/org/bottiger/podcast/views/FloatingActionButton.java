package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;

/**
 * Created by apl on 12-02-2015.
 */
public class FloatingActionButton extends android.support.design.widget.FloatingActionButton {

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

    public void onPaletteFound(ColorExtractor extractor) {
        int color = ColorUtils.adjustToTheme(mContext.getResources(), extractor.getPrimary());
        setBackgroundTintList(ColorStateList.valueOf(color));
        super.setRippleColor(extractor.getPrimaryTint());

        invalidate();
    }
}
