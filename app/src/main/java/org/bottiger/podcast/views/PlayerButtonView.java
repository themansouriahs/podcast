package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusObserver;
import org.bottiger.podcast.provider.FeedItem;

/**
 * TODO: document your custom view class.
 */
//public class PlayerButtonView extends com.melnykov.fab.FloatingActionButton implements PlayerStatusObserver, PaletteListener, DownloadObserver {
public class PlayerButtonView extends ImageButton implements PlayerStatusObserver, PaletteListener, DownloadObserver {

    public final static int STATE_DEFAULT = 0;
    public final static int STATE_DOWNLOAD = 1;
    public final static int STATE_DELETE = 2;

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;
    private long episodeId = -1;
    private int mProgress = 0;
    private int mLastProgress = 0;
    private static final int BITMAP_OFFSET = 5;
    private static final float RECTANGLE_SCALING = 1F;

    private Paint baseColorPaint;
    private Paint foregroundColorPaint;
    private RectF buttonRectangle;
    private RectF buttonRectangleBitmap;

    private Context mContext;
    private Bitmap s_Icon;
    private int defaultIcon;

    private int mCurrentState = 0;
    private SparseIntArray mStateIcons= new SparseIntArray();

    private PaletteObservable mCurrentListener;

    private int mForegroundColor = Color.CYAN;
    private int mBackgroundColor = Color.RED;

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

        baseColorPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        //baseColorPaint.setColor(mBackgroundColor);
        baseColorPaint.setColor(-1761607680);
        baseColorPaint.setTextSize(12.0F);
        baseColorPaint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        baseColorPaint.setStrokeWidth(10F);
        baseColorPaint.setAntiAlias(true);

        foregroundColorPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        foregroundColorPaint.setColor(mForegroundColor);
        foregroundColorPaint.setTextSize(12.0F);
        foregroundColorPaint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        foregroundColorPaint.setStrokeWidth(10F);
        foregroundColorPaint.setAntiAlias(true);

        buttonRectangle = new RectF();
        buttonRectangleBitmap = new RectF();

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
                s_Icon = BitmapFactory.decodeResource(getResources(), image);
            }
        }
        defaultIcon = image;
        mStateIcons.put(PlayerButtonView.STATE_DEFAULT, defaultIcon);
    }

    public synchronized void setEpisodeId(long argId) {
        this.episodeId = argId;
        ensureEpisode();
        PaletteObservable.registerListener(this);
    }

    public synchronized void unsetEpisodeId() {
        this.episodeId = -1;
        PaletteObservable.unregisterListener(this);
    }

    public void setStatus(PlayerStatusObservable.STATUS argStatus) {
        mStatus = argStatus;
        this.invalidate();
    }

    public void setColor(int argColor) {
        mBackgroundColor = argColor;
        this.invalidate();
    }

    public void setImage(int argImage) {
        s_Icon = BitmapFactory.decodeResource(getResources(), argImage);

        this.setImageBitmap(s_Icon);
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
        float halfW = getWidth()*RECTANGLE_SCALING;

        int left = (int)0;
        int width = (int)(halfW);
        int top = (int)0;

        buttonRectangle.set(left+BITMAP_OFFSET, top+BITMAP_OFFSET, left + width-BITMAP_OFFSET, top + width-BITMAP_OFFSET);

        //canvas.drawArc(buttonRectangle, -90, 360, true, baseColorPaint);

        if(mProgress!=0) {
            canvas.drawArc(buttonRectangle, -90, Math.round(360 * mProgress / 100F), true, foregroundColorPaint);
        }

        buttonRectangleBitmap = buttonRectangle;
        buttonRectangleBitmap.bottom -= BITMAP_OFFSET;
        buttonRectangleBitmap.top -= BITMAP_OFFSET;
        buttonRectangleBitmap.left -= BITMAP_OFFSET;
        buttonRectangleBitmap.right -= BITMAP_OFFSET;

        if (s_Icon == null) {
            Log.w("PlayerButton", "s_Icon is null");
            return;
        }

        //canvas.drawBitmap(s_Icon, null, buttonRectangle, baseColorPaint);

        long endTime = System.currentTimeMillis();
        long drawTime = endTime-startTime;
        String redundantString = mLastProgress == mProgress ? "(Progress unchanged) " : "(Progress diff: " + (mProgress-mLastProgress) + ") ";
        //Log.d("PlayButtonView", "onDraw: " + drawTime + " ms " + redundantString);

        mLastProgress = mProgress;

        super.onDraw(canvas);
    }

    @Override
    public FeedItem getEpisode() {
        ensureEpisode();

        Log.d("Optimize", "Please optimize PlayButtonView.getEpisode");
        return FeedItem.getById(this.getContext().getContentResolver(), episodeId);
    }

    @Override
    public void setProgressMs(long progressMs) {

    }

    @Override
    public void onStateChange(EpisodeStatus argStatus) {

    }

    @Override
    public void setProgressPercent(int argProgress) {
        mProgress = argProgress;
        if (mDownloadCompletedCallback != null && mProgress == 100) {
            mDownloadCompletedCallback.FileComplete();
            setState(PlayerButtonView.STATE_DELETE);
        }
        this.invalidate();
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        Palette.Swatch swatch = argChangedPalette.getVibrantSwatch();

        int c = swatch.getBodyTextColor();
        baseColorPaint.setColor(c); // -1761607680
        //foregroundColorPaint.setColor(swatch.getBodyTextColor());
        foregroundColorPaint.setColor(c);
        //super.setColorNormal(PlayerButtonView.ButtonColor(argChangedPalette));
        setBackgroundColor(PlayerButtonView.ButtonColor(argChangedPalette));
        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return getEpisode().getImageURL(getContext()); // FIXME
    }

    public interface DownloadStatus {
        void FileComplete();
        void FileDeleted();
    }
    private DownloadStatus mDownloadCompletedCallback = null;

    public void addDownloadCompletedCallback(DownloadStatus argCallback) {
        mDownloadCompletedCallback = argCallback;
    }

    public static int ButtonColor(@NonNull Palette argPalette) {
        Palette.Swatch color = argPalette.getVibrantSwatch();

        if (color==null)
            return Color.RED;

        return color.getRgb();
    }

    private void ensureEpisode() {
        if (episodeId < 0) {
            throw new IllegalStateException("Episode ID must be set before calling getEpisode");
        }
    }
}
