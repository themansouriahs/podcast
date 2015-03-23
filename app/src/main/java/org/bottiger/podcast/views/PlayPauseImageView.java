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
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusObserver;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.ColorExtractor;

/**
 * TODO: document your custom view class.
 */
// imageview
public class PlayPauseImageView extends ImageView implements PlayerStatusObserver,
                                                             PaletteListener,
                                                             DownloadObserver,
                                                             View.OnClickListener {

    private static final boolean DRAW_PROGRESS          = true;
    private static final boolean DRAW_PROGRESS_MARKER   = true;

    private static final int START_ANGLE = -90;
    private static final int DRAW_OFFSET = 5;

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;

    private FeedItem mEpisode;
    private long mEpisodeId = -1;

    private Context mContext;

    private RectF bounds;
    private Rect boundsRound = new Rect();

    private int mProgressPercent = 0;

    private static Bitmap s_playIcon;
    private static Bitmap s_pauseIcon;

    private Paint paint;
    private Paint paintBorder;

    private int mPaintColor = Color.BLACK; // TODO: use a default from R.color...
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
        paintBorder.setStrokeWidth(5F);

        setOnClickListener(this);
        if (s_playIcon == null || s_pauseIcon == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            s_playIcon = BitmapFactory.decodeResource(getResources(), R.drawable.av_play, options);
            s_pauseIcon = BitmapFactory.decodeResource(getResources(), R.drawable.av_pause, options);
        }
        PlayerStatusObservable.registerListener(this);
    }

    @Override
    public FeedItem getEpisode() {
        if (mEpisode == null || mEpisode.getId() != mEpisodeId)
            mEpisode = FeedItem.getById(this.getContext().getContentResolver(), mEpisodeId);

        return mEpisode;
    }

    public synchronized void setEpisodeId(long argId) {
        this.mEpisodeId = argId;
        PaletteObservable.registerListener(this);

        long offset = getEpisode().offset > 0 ? getEpisode().offset : 0;
        setProgressMs(offset);
        invalidate();
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisodeId = -1;
        PaletteObservable.unregisterListener(this);
    }

    public void setStatus(PlayerStatusObservable.STATUS argStatus) {
        mStatus = argStatus;
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
        float radius = centerX-DRAW_OFFSET;
        canvas.drawCircle(centerX,centerY,radius,paint);

        int diff2 = (int) (centerY-radius);
        boolean updateOutline = bounds == null;

        bounds = new RectF(DRAW_OFFSET, diff2, contentWidth - DRAW_OFFSET, contentWidth - DRAW_OFFSET + diff2); // DRAW_OFFSET-diff

        if (updateOutline) {
            onSizeChanged(0,0,0,0);
        }

        if (DRAW_PROGRESS) {
            canvas.drawArc(bounds, START_ANGLE, getProgressAngle(mProgressPercent), false, paintBorder);
        }

        // Draw the play/pause icon
        Bitmap icon = mStatus == PlayerStatusObservable.STATUS.PLAYING ?  s_pauseIcon : s_playIcon;

        int bitmapx = centerX-icon.getWidth()/2;
        int bitmapy = centerY-icon.getHeight()/2;
        canvas.drawBitmap(icon, bitmapx, bitmapy, paint);
    }

    @Override
    public void setProgressMs(long progressMs) {

        // copy from seekbar

        if (progressMs < 0) {
            throw new IllegalStateException("Progress must be positive");
        }
        float progress = 0;
        float duration = getEpisode().getDuration();

        if (duration <= 0) {
            Log.d("Warning", "Seekbar state may be invalid");
            return;
        }

        try {
            progress = progressMs / duration * 100;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgressPercent((int) progress);
    }

    @Override
    public void onStateChange(EpisodeStatus argStatus) {
        if (argStatus.getEpisodeId() != mEpisodeId) {
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
        boolean isPlaying = PodcastBaseFragment.mPlayerServiceBinder.toggle(mEpisodeId);

        setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.STOPPED);

        FeedItem item = getEpisode();
        Playlist playlist = Playlist.getActivePlaylist();

        if (playlist.contains(item)) {
            int position = playlist.getPosition(item);

            if (position != 0) {
                playlist.move(position, 0);
                playlist.notifyPlaylistRangeChanged(position, 0);
            }
        } else {
            playlist.setItem(0, item);
            playlist.notifyPlaylistRangeChanged(0, 0);
        }
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(mContext, argChangedPalette);
        setColor(extractor.getPrimary(), extractor.getSecondary());
    }

    @Override
    public String getPaletteUrl() {
        return FeedItem.getById(getContext().getContentResolver(), mEpisodeId).getImageURL(getContext()); // FIXME
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


    //
    // http://stackoverflow.com/questions/27497987/android-elevation-is-not-showing-a-shadow-under-a-customview
    //

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
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
}
