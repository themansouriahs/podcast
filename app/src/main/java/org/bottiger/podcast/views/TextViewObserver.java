package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.Chronometer;

import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.provider.IEpisode;

import static android.os.SystemClock.elapsedRealtime;

/**
 * Created by apl on 25-03-2015.
 */
public class TextViewObserver extends Chronometer implements ExoPlayerWrapper.Listener {

    protected IEpisode mEpisode = null;
    private boolean mIsTicking = false;

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

    @Override
    public void onStateChanged(boolean playWhenReady, @SoundWavesPlayerBase.PlayerState int playbackState) {

        // This happens when the playlist is empty and an episode is about to be played.
        // The state change fires prior to the binding.
        if (mEpisode == null)
            return;

        long base = elapsedRealtime() - mEpisode.getOffset();
        setBase(base);
        if (playWhenReady && !mIsTicking) {
            start();
        } else if (!playWhenReady && mIsTicking) {
            stop();
        }
        mIsTicking = playWhenReady;
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

    }
}
