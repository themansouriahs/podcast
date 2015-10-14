package org.bottiger.podcast.views.MultiShrink.feed;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.ImageViewTinted;

/**
 * An {@link ImageView} designed to display QuickContact's contact photo. When requested to draw
 * {@link LetterTileDrawable}'s, this class instead draws a different default avatar drawable.
 *
 * In addition to supporting {@link ImageView#setColorFilter} this also supports a {@link #setTint}
 * method.
 *
 * This entire class can be deleted once use of LetterTileDrawable is no longer used
 * inside QuickContactsActivity at all.
 */
public class FeedViewTopImage extends ImageViewTinted {

    private Drawable mOriginalDrawable;
    private BitmapDrawable mBitmapDrawable;
    private int mTintColor;

    public FeedViewTopImage(Context context) {
        this(context, null);
    }

    public FeedViewTopImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FeedViewTopImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTint(int color) {
        if (mBitmapDrawable == null || mBitmapDrawable.getBitmap() == null
                || mBitmapDrawable.getBitmap().hasAlpha()) {
            setBackgroundColor(color);
        } else {
            if (Build.VERSION.SDK_INT >= 16) {
                setBackground(null);
            }
        }
        mTintColor = color;
        postInvalidate();
    }

    @Override
    public Drawable getDrawable() {
        return mOriginalDrawable;
    }
}
