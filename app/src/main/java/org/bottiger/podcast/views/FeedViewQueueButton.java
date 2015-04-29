package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;

/**
 * Created by apl on 19-04-2015.
 */
public class FeedViewQueueButton extends PlayPauseImageView {

    private static Bitmap s_queueIcon;

    public FeedViewQueueButton(Context context) {
        super(context);
        init(context);
    }

    public FeedViewQueueButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FeedViewQueueButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context argContext) {

        setOnClickListener(this);
        if (s_queueIcon == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            s_queueIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_add_white, options);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int contentWidth = getWidth();
        int contentHeight = getHeight();

        int centerX = contentWidth/2;
        int centerY = contentHeight/2;

        // Draw the play/pause icon
        Bitmap icon = s_queueIcon;

        int bitmapx = centerX-icon.getWidth()/2;
        int bitmapy = centerY-icon.getHeight()/2;

        canvas.drawBitmap(icon, bitmapx, bitmapy, paint);
    }

    @Override
    public void onClick(View view) {
        FeedItem item = getEpisode();
        Playlist playlist = Playlist.getActivePlaylist();

        playlist.queue(mContext, item);
    }

    @Override
    protected boolean drawIcon() {
        return false;
    }
}
