package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusObserver;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.StrUtils;

/**
 * Created by apl on 25-03-2015.
 */
public class TextViewObserver extends TextView implements PlayerStatusObserver {

    protected FeedItem mEpisode = null;

    public TextViewObserver(Context context) {
        super(context);
    }

    public TextViewObserver(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewObserver(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextViewObserver(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setEpisode(@NonNull FeedItem argEpisode) {
        if (mEpisode != null) {
            PlayerStatusObservable.unregisterListener(this);
        }
        mEpisode = argEpisode;
        PlayerStatusObservable.registerListener(this);
    }

    @Override
    public FeedItem getEpisode() {
        return mEpisode;
    }

    @Override
    public void setProgressMs(long progressMs) {
        if (progressMs < 0)
            return;

        setText(StrUtils.formatTime(progressMs));
        invalidate();
    }

    @Override
    public void onStateChange(EpisodeStatus argStatus) {
        setProgressMs(argStatus.getPlaybackPositionMs());
    }
}
