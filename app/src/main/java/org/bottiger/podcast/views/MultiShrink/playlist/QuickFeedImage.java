package org.bottiger.podcast.views.MultiShrink.playlist;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.bottiger.podcast.R;

/**
 * An {@link android.widget.ImageView} designed to display QuickContact's contact photo. When requested to draw
 * {@link LetterTileDrawable}'s, this class instead draws a different default avatar drawable.
 *
 * In addition to supporting {@link android.widget.ImageView#setColorFilter} this also supports a {@link #setTint}
 * method.
 *
 * This entire class can be deleted once use of LetterTileDrawable is no longer used
 * inside QuickContactsActivity at all.
 */
public class QuickFeedImage extends ImageView {

    private Drawable mOriginalDrawable;
    private BitmapDrawable mBitmapDrawable;
    private int mTintColor;
    private boolean mIsBusiness;

    public QuickFeedImage(Context context) {
        this(context, null);
    }

    public QuickFeedImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QuickFeedImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public QuickFeedImage(Context context, AttributeSet attrs, int defStyleAttr,
                          int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    public boolean isBasedOffLetterTile() {
        return mOriginalDrawable instanceof DrawableContainer;
    }

    public void setIsBusiness(boolean isBusiness) {
        mIsBusiness = isBusiness;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        // There is no way to avoid all this casting. Blending modes aren't equally
        // supported for all drawable types.
        BitmapDrawable bitmapDrawable;

        bitmapDrawable = (BitmapDrawable) drawable;

        if (bitmapDrawable == null) {
            bitmapDrawable = (BitmapDrawable) getResources().getDrawable(
                    R.drawable.generic_podcast);
        }

        mOriginalDrawable = drawable;
        mBitmapDrawable = bitmapDrawable;
        setTint(mTintColor);
        super.setImageDrawable(bitmapDrawable);
    }


    @Override
    public Drawable getDrawable() {
        return mOriginalDrawable;
    }
}
