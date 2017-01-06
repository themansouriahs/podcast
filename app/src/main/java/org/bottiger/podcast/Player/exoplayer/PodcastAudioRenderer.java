package org.bottiger.podcast.player.exoplayer;

import android.media.audiofx.AutomaticGainControl;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.MediaSource;

import org.bottiger.podcast.utils.AndroidUtil;

/**
 * Custom renderer used for PremoFM
 * Created by evanhalley on 12/4/15.
 *
 * https://github.com/emuneee/premofm/blob/master/LICENSE
 */
public class PodcastAudioRenderer extends MediaCodecAudioRenderer {

    private static final long US_IN_MS = 1_000;

    private boolean mRemoveSilence = false;
    private boolean mAutomaticGainControl = false;

    @Nullable
    private Integer mAudioSessionId = null;

    private ProgressUpdateListener mProgressUpdateListener;
    private long mProgress;


    public PodcastAudioRenderer(MediaSource source) {
        super((MediaCodecSelector) source);
        //super(source, MediaCodecSelector.DEFAULT);
    }


    public void setProgressUpdateListener(ProgressUpdateListener progressUpdateListener) {
        mProgressUpdateListener = progressUpdateListener;
    }

    public boolean doRemoveSilence() {
        return mRemoveSilence;
    }

    public void setRemoveSilence(boolean argRemoveSilence) {
        this.mRemoveSilence = argRemoveSilence;
    }

    public void setAutomaticGainControl(boolean argAutomaticGainControl) {
        this.mAutomaticGainControl = argAutomaticGainControl;
    }

    public boolean getAutomaticGainControl() {
        return mAutomaticGainControl;
    }

    /*
    @Override
    protected void doSomeWork(long positionUs, long elapsedRealtimeUs, boolean sourceIsReady) throws ExoPlaybackException {
        super.doSomeWork(positionUs, elapsedRealtimeUs, sourceIsReady);
        long progress = positionUs / US_IN_MS;
        long duration = getDurationUs() / US_IN_MS;
        long bufferedProgress = getBufferedPositionUs();

        // if the end of the track has been buffered, set progress to duration
        if (bufferedProgress == Renderer.END_OF_TRACK_US) {
            bufferedProgress = duration;
        } else {
            bufferedProgress = bufferedProgress / US_IN_MS;
        }

        if (mProgress != progress) {
            mProgress = progress;

            if (mProgressUpdateListener != null) {
                mProgressUpdateListener.onProgressUpdate(progress, bufferedProgress, duration);
            }
        }
    }
    */

    /**
     * Invoked when the audio session id becomes known. Once the id is known it will not change
     * (and hence this method will not be invoked again) unless the renderer is disabled and then
     * subsequently re-enabled.
     * <p>
     * The default implementation is a no-op. One reason for overriding this method would be to
     * instantiate and enable a {@link Virtualizer} in order to spatialize the audio channels. For
     * this use case, any {@link Virtualizer} instances should be released in {@link #onDisabled()}
     * (if not before).
     *
     * @param audioSessionId The audio session id.
     */
    @Override
    protected void onAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;

        if (mAutomaticGainControl && AndroidUtil.SDK_INT >= 16) {
            AutomaticGainControl.create(audioSessionId);
        }
    }
}