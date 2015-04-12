package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ColorExtractor;

/**
 * Created by apl on 12-04-2015.
 */
public class TintedFramelayout extends FrameLayout implements PaletteListener {

    private Context mContext;
    private Subscription mSubscription;

    public TintedFramelayout(Context context) {
        super(context);
        mContext = context;
    }

    public TintedFramelayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public TintedFramelayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TintedFramelayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    public void setSubscription(Subscription mSubscription) {
        this.mSubscription = mSubscription;
        PaletteObservable.registerListener(this);
    }

    public void unsetSubscription() {
        mSubscription = null;
        PaletteObservable.unregisterListener(this);
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor colorExtractor = new ColorExtractor(argChangedPalette);

        boolean needsUpdate = false;

        if (colorExtractor.getPrimary() != mSubscription.getPrimaryColor()) {
            mSubscription.setPrimaryColor(colorExtractor.getPrimary());
            needsUpdate = true;
        }

        if (colorExtractor.getSecondary() != mSubscription.getSecondaryColor()) {
            mSubscription.setSecondaryColor(colorExtractor.getSecondary());
            needsUpdate = true;
        }

        if (colorExtractor.getPrimaryTint() != mSubscription.getPrimaryTintColor()) {
            mSubscription.setPrimaryTintColor(colorExtractor.getPrimaryTint());
            needsUpdate = true;
        }

        if (needsUpdate) {
            new Thread(new Runnable() {
                public void run() {
                    mSubscription.update(mContext.getContentResolver());
                }
            }).start();
        }

        setBackgroundColor(mSubscription.getPrimaryColor());
    }

    @Override
    public String getPaletteUrl() {
        return mSubscription.getImageURL();
    }
}
