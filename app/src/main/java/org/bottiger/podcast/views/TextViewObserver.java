package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PlayerStatusProgressData;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.StrUtils;

/**
 * Created by apl on 25-03-2015.
 */
public class TextViewObserver extends TextView {

    protected IEpisode mEpisode = null;

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

    public void setEpisode(@NonNull IEpisode argEpisode) {
        mEpisode = argEpisode;
    }

    public IEpisode getEpisode() {
        return mEpisode;
    }

    @Subscribe
    public void setProgressMs(PlayerStatusProgressData argPlayerProgress) {
        if (argPlayerProgress.progressMs < 0)
            return;

        setText(StrUtils.formatTime(argPlayerProgress.progressMs));
        invalidate();
    }

    public void onStateChange(EpisodeStatus argStatus) {
        setProgressMs(new PlayerStatusProgressData(argStatus.getPlaybackPositionMs()));
    }
}
