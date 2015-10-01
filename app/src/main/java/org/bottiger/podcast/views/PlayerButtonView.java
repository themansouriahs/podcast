package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ImageButton;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;

import java.lang.ref.WeakReference;

/**
 * TODO: document your custom view class.
 */
public class PlayerButtonView extends ImageButton implements PaletteListener  {

    public final static int STATE_DEFAULT = 0;
    public final static int STATE_DOWNLOAD = 1;
    public final static int STATE_DELETE = 2;
    public final static int STATE_QUEUE = 3;

    private @PlayerStatusObservable.PlayerStatus int mStatus = PlayerStatusObservable.STOPPED;
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
                s_Icon = new WeakReference<Bitmap>(BitmapFactory.decodeResource(getResources(), image));
            }
        }
        defaultIcon = image;
        mStateIcons.put(PlayerButtonView.STATE_DEFAULT, defaultIcon);
    }

    public synchronized void setEpisode(IEpisode argEpisode) {
        this.mEpisode = argEpisode;
        ensureEpisode();
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisode = null;
    }

    public void setStatus(@PlayerStatusObservable.PlayerStatus int argStatus) {
        mStatus = argStatus;
        this.invalidate();
    }

    public void setColor(int argColor) {
        mBackgroundColor = argColor;
        this.invalidate();
    }

    public void setImage(int argImage) {
        s_Icon = new WeakReference<Bitmap>(BitmapFactory.decodeResource(getResources(), argImage));

        this.setImageBitmap(s_Icon.get());
        this.invalidate();
    }

    public void addState(int argState, int argDrawable) {
        if (mStateIcons.get(argState) != 0) {
            Log.w("PlayerButtonView", "Mapping already exists. Overwriting");
        }

        mStateIcons.put(argState, argDrawable);
    }

    public int getState() {
        return mCurrentState;
    }

    public void setState(int argState) {
        if (mStateIcons.get(argState) == 0) {
            throw new IllegalStateException("No such state exists");
        }

        mCurrentState = argState;
        setImage(mStateIcons.get(argState, defaultIcon));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long startTime = System.currentTimeMillis();
        super.onDraw(canvas);
    }

    protected int getProgress() {
        return mProgress;
    }

    public IEpisode getEpisode() {
        ensureEpisode();

        return mEpisode;
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        int color = ColorUtils.getTextColor(getContext());
        ColorExtractor extractor = new ColorExtractor(argChangedPalette, color);

        mBaseColorPaint.setColor(extractor.getSecondary()); // -1761607680
        mForegroundColorPaint.setColor(extractor.getSecondary());

        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return mEpisode.getArtwork(mContext).toString();
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
            throw new IllegalStateException("Episode ID must be set before calling getEpisode");
        }
    }
}
