package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ImageButton;

import org.bottiger.podcast.R;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;

import java.lang.ref.WeakReference;

/**
 * TODO: document your custom view class.
 */
public class PlayerButtonView extends ImageButton  {

    public final static int STATE_DEFAULT = 0;
    public final static int STATE_DOWNLOAD = 1;
    public final static int STATE_DELETE = 2;
    public final static int STATE_QUEUE = 3;

    private IEpisode mEpisode;

    protected Paint mBaseColorPaint;
    protected Paint mForegroundColorPaint;

    private Context mContext;
    private WeakReference<Bitmap> s_Icon;
    private int defaultIcon;

    private int mCurrentState = 0;
    private SparseIntArray mStateIcons= new SparseIntArray();

    protected int mProgress = 0;
    protected DownloadStatus mDownloadCompletedCallback = null;

    private int mForegroundColor = getResources().getColor(R.color.colorPrimaryDark);
    private int mBackgroundColor = getResources().getColor(R.color.colorPrimaryDark);

    public PlayerButtonView(Context context) {
        super(context);
        init(context, null);
    }

    public PlayerButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PlayerButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        mBaseColorPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        mBaseColorPaint.setColor(mBackgroundColor);
        mBaseColorPaint.setTextSize(12.0F);
        mBaseColorPaint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        mBaseColorPaint.setStrokeWidth(10F);
        mBaseColorPaint.setAntiAlias(true);

        mForegroundColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mForegroundColorPaint.setColor(mForegroundColor);
        mForegroundColorPaint.setTextSize(12.0F);
        mForegroundColorPaint.setStyle(Paint.Style.STROKE); // Paint.Style.FILL_AND_STROKE
        mForegroundColorPaint.setStrokeWidth(10F);
        mForegroundColorPaint.setAntiAlias(true);

        mContext = context;

        int image = -1;
        if (s_Icon == null) {
            TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.PlayerButtonViewImage, 0, 0);
            try {
                image = typedArray.getResourceId(R.styleable.PlayerButtonViewImage_image, R.drawable.generic_podcast);
            } finally {
                typedArray.recycle();
            }

            if (image > 0) {
                s_Icon = new WeakReference<>(BitmapFactory.decodeResource(getResources(), image));
            }
        }
        defaultIcon = image;

        if (defaultIcon == 0) {
            VendorCrashReporter.report("Default icon 0", "Should not happen");
            defaultIcon = R.drawable.generic_podcast;
        }

        mStateIcons.put(PlayerButtonView.STATE_DEFAULT, defaultIcon);
    }

    public synchronized void setEpisode(IEpisode argEpisode) {
        this.mEpisode = argEpisode;
        ensureEpisode();
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisode = null;
    }

    public void setColor(int argColor) {
        mBackgroundColor = argColor;
        this.invalidate();
    }

    public void setImage(int argImage) {
        this.setImageResource(argImage);
        this.invalidate();
    }

    public void addState(int argState, @DrawableRes int argDrawable) {
        if (mStateIcons.get(argState) != 0) {
            Log.w("PlayerButtonView", "Mapping already exists. Overwriting");
        }

        mStateIcons.put(argState, argDrawable);
    }

    public int getState() {
        return mCurrentState;
    }

    public void setState(@DrawableRes int argState) {
        if (mStateIcons.get(argState) == 0) {
            VendorCrashReporter.report("No such state exists", "No such state exists: " + argState);
            return;
        }

        mCurrentState = argState;
        setImageResource(mStateIcons.get(argState, defaultIcon));
    }

    protected int getProgress() {
        return mProgress;
    }

    @Nullable
    public IEpisode getEpisode() {
        ensureEpisode();

        return mEpisode;
    }

    public void onPaletteFound(ColorExtractor extractor) {
        int color = ColorUtils.getTextColor(getContext());

        int colorFinal = ColorUtils.adjustToTheme(this.mContext.getResources(), mEpisode.getSubscription(mContext));

        //colorFinal = Color.BLACK;

        mBaseColorPaint.setColor(colorFinal); // -1761607680
        mForegroundColorPaint.setColor(colorFinal);

        invalidate();
    }

    public @ColorInt int getForegroundColor(@NonNull ColorExtractor extractor) {
        return extractor.getSecondary();
    }

    public interface DownloadStatus {
        void FileComplete();
        void FileDeleted();
    }

    public void addDownloadCompletedCallback(DownloadStatus argCallback) {
        mDownloadCompletedCallback = argCallback;
    }

    public static int StaticButtonColor(@Nullable Context argContext, @NonNull Palette argPalette, @ColorInt int argBaseColor) {
        ColorExtractor extractor = new ColorExtractor(argContext, argPalette, argBaseColor);
        return extractor.getPrimary();
    }

    public int ButtonColor(@NonNull Palette argPalette) {
        int color = ColorUtils.getTextColor(getContext());
        return StaticButtonColor(mContext, argPalette, color);
    }

    private void ensureEpisode() {
        if (mEpisode == null) {
            try {
                VendorCrashReporter.handleException(new IllegalStateException("Episode ID must be set before calling getEpisode"));
            } catch (IllegalStateException ise) {

            }
        }
    }
}
