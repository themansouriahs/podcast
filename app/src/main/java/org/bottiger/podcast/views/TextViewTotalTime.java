package org.bottiger.podcast.views;

import android.content.Context;
import android.util.AttributeSet;

import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.utils.StrUtils;

/**
 * Created by apl on 25-03-2015.
 */
public class TextViewTotalTime extends TextViewObserver {
    public TextViewTotalTime(Context context) {
        super(context);
    }

    public TextViewTotalTime(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewTotalTime(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TextViewTotalTime(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setProgressMs(long progressMs) {
        return;
    }

    public void onStateChange(EpisodeStatus argStatus) {
        long progressMs = mEpisode.getDuration();

        if (progressMs < 0)
            return;

        setText(StrUtils.formatTime(progressMs));
        invalidate();
    }
}
