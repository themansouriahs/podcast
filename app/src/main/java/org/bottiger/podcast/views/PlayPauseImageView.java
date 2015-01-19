package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusObserver;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;

/**
 * TODO: document your custom view class.
 */
// imageview
public class PlayPauseImageView extends ImageView implements PlayerStatusObserver, PaletteListener, DownloadObserver, View.OnClickListener {

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;
    private long mEpisodeId = -1;

    private static Bitmap s_playIcon;
    private static Bitmap s_pauseIcon;

    private Paint paint;

    private int mPaintColor = Color.GREEN; // TODO: use a default from R.color...

    public PlayPauseImageView(Context context) {
        super(context);
        init();
    }

    public PlayPauseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayPauseImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint(Paint.LINEAR_TEXT_FLAG);
        paint.setColor(mPaintColor);
        paint.setTextSize(12.0F);
        paint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        paint.setStrokeWidth(10F);

        setOnClickListener(this);
        if (s_playIcon == null || s_pauseIcon == null) {
            s_playIcon = BitmapFactory.decodeResource(getResources(), R.drawable.av_play);
            s_pauseIcon = BitmapFactory.decodeResource(getResources(), R.drawable.av_pause);
        }
        PlayerStatusObservable.registerListener(this);
    }

    public void setmEpisodeId(long argId) {
        this.mEpisodeId = argId;
    }

    public void setStatus(PlayerStatusObservable.STATUS argStatus) {
        mStatus = argStatus;
        this.invalidate();
    }

    public void setColor(int argColor) {
        //mPaintColor = argColor;
        paint.setColor(argColor);
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {


        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth(); //  - paddingLeft - paddingRight
        int contentHeight = getHeight(); //  - paddingTop - paddingBottom

        int centerX = contentWidth/2;
        int centerY = contentHeight/2;

        // Draw the background circle
        canvas.drawCircle(centerX,centerY,centerX-5,paint);

        // Draw the play/pause icon
        Bitmap icon = mStatus == PlayerStatusObservable.STATUS.PLAYING ?  s_pauseIcon : s_playIcon;

        //BitmapDrawable drawable = new BitmapDrawable(getResources(), icon);

        int bitmapx = centerX-icon.getWidth()/2;
        int bitmapy = centerY-icon.getHeight()/2;
        canvas.drawBitmap(icon, bitmapx, bitmapy, paint);

        //super.onDraw(canvas);
    }

    @Override
    public FeedItem getEpisode() {
        return FeedItem.getById(this.getContext().getContentResolver(), mEpisodeId);
    }

    @Override
    public void setProgressMs(long progressMs) {

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
        setColor(PlayerButtonView.ButtonColor(argChangedPalette));
        invalidate();
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
}
