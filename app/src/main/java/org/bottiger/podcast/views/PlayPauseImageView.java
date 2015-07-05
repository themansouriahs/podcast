package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.squareup.otto.Subscribe;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusData;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusProgressData;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.ColorExtractor;

/**
 * TODO: document your custom view class.
 */
public class PlayPauseImageView extends ImageButton implements PaletteListener,
                                                             DownloadObserver,
                                                             View.OnClickListener {

    private static final boolean DRAW_PROGRESS          = true;
    private static final boolean DRAW_PROGRESS_MARKER   = true;

    public enum LOCATION { PLAYLIST, FEEDVIEW, DISCOVERY_FEEDVIEW, OTHER };
    private LOCATION mLocation = LOCATION.OTHER;

    private static final int START_ANGLE = -90;
    private static final int DRAW_OFFSET = 6;
    private static final int DRAW_WIDTH = 6;

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;

    private IEpisode mEpisode;

    protected Context mContext;

    private RectF bounds;
    private Rect boundsRound = new Rect();

    private int mProgressPercent = 0;

    protected Paint paint;
    private Paint paintBorder;

    private int mPaintColor = getResources().getColor(R.color.colorPrimaryDark);
    private int mPaintBorderColor = Color.WHITE;

    public PlayPauseImageView(Context context) {
        super(context);
        init(context);
    }

    public PlayPauseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayPauseImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context argContext) {

        mContext = argContext;

        paint = new Paint(Paint.LINEAR_TEXT_FLAG);
        paint.setColor(mPaintColor);
        paint.setTextSize(12.0F);
        paint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        paint.setStrokeWidth(1F);

        paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setColor(mPaintBorderColor);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(DRAW_WIDTH);

        setScaleType(ScaleType.CENTER);

        if (Build.VERSION.SDK_INT >= 16) {
            setBackground(null);
        }

        if (isInEditMode()) {
            return;
        }

        setOnClickListener(this);
    }

    @NonNull
    @Override
    public IEpisode getEpisode() {
        return mEpisode;
    }

    public synchronized void setEpisode(IEpisode argEpisode, LOCATION argLocation) {
        this.mLocation = argLocation;
        this.mEpisode = argEpisode;


        long offset = mEpisode instanceof FeedItem && ((FeedItem)mEpisode).offset > 0 ? ((FeedItem)mEpisode).offset : 0;
        setProgressMs(new PlayerStatusProgressData(offset));
        invalidate();
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisode = null;
    }

    public void setStatus(PlayerStatusObservable.STATUS argStatus) {
        mStatus = argStatus;

        int resid;
        if (mStatus == PlayerStatusObservable.STATUS.PLAYING) {
            resid = R.drawable.av_pause;
        } else {
            resid = R.drawable.av_play;
        }
        setImageResource(resid);

        this.invalidate();
    }

    public void setColor(int argColor, int argOuterColor) {
        paint.setColor(argColor);
        paintBorder.setColor(argOuterColor);
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int contentWidth = getWidth();
        int contentHeight = getHeight();

        int centerX = contentWidth/2;
        int centerY = contentHeight/2;

        // Draw the background circle
        float radius = centerX-DRAW_WIDTH;

        if (drawBackground()) {
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        int diff2 =  DRAW_WIDTH;//(int) (centerY-radius);
        boolean updateOutline = bounds == null;

        bounds = new RectF(DRAW_OFFSET, diff2, contentWidth - DRAW_OFFSET, contentWidth - diff2); // DRAW_OFFSET-diff

        if (updateOutline) {
            onSizeChanged(0,0,0,0);
        }

        if (DRAW_PROGRESS && mProgressPercent > 0) {
            canvas.drawArc(bounds, START_ANGLE, getProgressAngle(mProgressPercent), false, paintBorder);
        }

        super.onDraw(canvas);
    }

    @Subscribe
    public void setProgressMs(PlayerStatusProgressData argPlayerProgress) {

        // copy from seekbar

        if (argPlayerProgress.progressMs < 0) {
            throw new IllegalStateException("Progress must be positive");
        }
        float progress = 0;

        IEpisode episode = getEpisode();

        if (episode == null)
            return;

        float duration = episode.getDuration();

        if (duration <= 0) {
            Log.d("Warning", "Seekbar state may be invalid");
            return;
        }

        try {
            progress = argPlayerProgress.progressMs / duration * 100;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgressPercent((int) progress);
    }

    @Subscribe
    public void onPlayerStateChange(PlayerStatusData argPlayerStatus) {
        if (argPlayerStatus == null)
            return;

        if (!getEpisode().equals(argPlayerStatus.episode)) {

            setStatus(PlayerStatusObservable.STATUS.PAUSED);
            return;
        }

        setStatus(argPlayerStatus.status);
    }

    public void onStateChange(EpisodeStatus argStatus) {
        if (!argStatus.getEpisode().equals(mEpisode)) {
            return;
        }

        if (mStatus == argStatus.getStatus()) {
            return;
        }

        setStatus(argStatus.getStatus());
    }

    @Override
    public void setProgressPercent(int argProgress) {
        mProgressPercent = argProgress;
        invalidate();
    }

    @Override
    public void onClick(View view) {

        IAnalytics.EVENT_TYPE type = getEventType();
        if (type != null) {
            SoundWaves.sAnalytics.trackEvent(type);
        }
        boolean isPlaying = MainActivity.sBoundPlayerService.toggle(mEpisode);

        setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.STOPPED);

        //SoundWaves.getBus().post(new PlaylistData().playlistChanged = true);
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(mContext, argChangedPalette);
        setColor(extractor.getPrimary(), extractor.getSecondary());
    }

    @Override
    public String getPaletteUrl() {
        return mEpisode.getUrl().toString();
    }

    private static int smallSize = -1;
    private static int largeSize = -1;

    public static int getSmallSize(@NonNull Context argContext) {
        if (smallSize > 0)
            return smallSize;

        smallSize = argContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size_normal);

        return smallSize;
    }

    public static int getLargeSize(@NonNull Context argContext) {
        if (largeSize > 0)
            return largeSize;

        largeSize = argContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size);

        return largeSize;
    }

    private float getProgressAngle(int argProgress) {
        return argProgress*3.6F;
    }

    @Nullable
    private IAnalytics.EVENT_TYPE getEventType() {
        if (mLocation == LOCATION.PLAYLIST) {
            return IAnalytics.EVENT_TYPE.PLAY_FROM_PLAYLIST;
        }

        if (mLocation == LOCATION.FEEDVIEW) {
            return IAnalytics.EVENT_TYPE.PLAY_FROM_FEEDVIEW;
        }

        if (mLocation == LOCATION.DISCOVERY_FEEDVIEW) {
            return IAnalytics.EVENT_TYPE.PLAY_FROM_FEEDVIEW;
        }

        return null;
    }


    //
    // http://stackoverflow.com/questions/27497987/android-elevation-is-not-showing-a-shadow-under-a-customview
    //

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if  (isInEditMode()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CustomOutline(bounds));
        }
    }

    @TargetApi(21)
    private class CustomOutline extends ViewOutlineProvider {

        RectF bounds = null;

        CustomOutline(RectF argRectF) {
            bounds = argRectF;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            if (bounds != null) {
                bounds.round(boundsRound);
                outline.setOval(boundsRound);
            }
        }
    }

    protected boolean drawIcon() {
        return true;
    }

    protected boolean drawBackground() {
        return true;
    }
}
