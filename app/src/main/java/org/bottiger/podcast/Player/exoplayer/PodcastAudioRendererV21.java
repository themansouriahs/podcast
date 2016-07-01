package org.bottiger.podcast.player.exoplayer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.util.Util;

import org.bottiger.podcast.utils.PlaybackSpeed;

import java.nio.ByteBuffer;

/**
 * Custom renderer used for PremoFM
 * Created by evanhalley on 12/4/15.
 */
public class PodcastAudioRendererV21 extends PodcastAudioRenderer {

    private static final String TAG = "PodcastAudioRendererV21";
    private static final boolean DEBUG = false;

    private static final int SAMPLES_PER_CODEC_FRAME = 1_024;

    private Sonic mSonic;
    private byte[] mSonicInputBuffer;
    private byte[] mSonicOutputBuffer;

    private SilenceRemover mSilenceRemover;

    private float mSpeed = PlaybackSpeed.DEFAULT;

    private int mLastSeenBufferIndex = -1;
    private ByteBuffer mLastInternalBuffer;
    private byte[] mSilenceInputBuffer;
    private byte[] mSilenceOutputBuffer;

    public PodcastAudioRendererV21(SampleSource source) {
        super(source);
    }

    public synchronized void setSpeed(float speed) {
        this.mSpeed = speed;
        if (this.mSonic != null)
            this.mSonic.setSpeed(speed);
    }

    public float getSpeed() {
        return mSpeed;
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec,
                                          ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo, int bufferIndex,
                                          boolean shouldSkip) throws ExoPlaybackException {

        if (DEBUG)
            Log.d(TAG, "processOutputBuffer");

        if (bufferIndex == mLastSeenBufferIndex) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec,
                    mLastInternalBuffer, bufferInfo, bufferIndex, shouldSkip);
        } else {
            mLastSeenBufferIndex = bufferIndex;
        }

        int bytesToRead;

        if (Util.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            buffer.position(0);
            bytesToRead = bufferInfo.size;
        } else {
            bytesToRead = buffer.remaining();
        }

        buffer.get(mSonicInputBuffer, 0, bytesToRead);

        int bytesOld = bytesToRead;

        // Remove Silence
        if (doRemoveSilence()) {
            mSilenceOutputBuffer = mSilenceRemover.removeSilence(mSonicInputBuffer, mSilenceOutputBuffer, bytesToRead);
            bytesToRead = mSilenceRemover.getOutputLength();
        } else {
            mSilenceOutputBuffer = mSonicInputBuffer;
        }

        long us_skipped = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            if (bufferInfo.size != 0) {
                long us_pr_frame = (bufferInfo.presentationTimeUs / bufferInfo.size);
                us_skipped = us_pr_frame * (bytesOld - bytesToRead);
            }
        }

        if (us_skipped > 0) {
            Log.w(TAG, "bytediff: " + (bytesOld - bytesToRead) + "time skipped: " + us_skipped);
        }

        positionUs += us_skipped;

        mSonic.writeBytesToStream(mSilenceOutputBuffer, bytesToRead);
        final int readThisTime = mSonic.readBytesFromStream(mSonicOutputBuffer, mSonicOutputBuffer.length);

        bufferInfo.offset = 0;
        mLastInternalBuffer.position(0);

        bufferInfo.size = readThisTime;
        mLastInternalBuffer.limit(readThisTime);

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, mLastInternalBuffer,
                bufferInfo, bufferIndex, shouldSkip);
    }

    @TargetApi(16)
    @Override
    protected void onOutputFormatChanged(final MediaCodec codec, final MediaFormat format) {
        super.onOutputFormatChanged(codec, format);

        if (DEBUG)
            Log.d(TAG, "onOutputFormatChanged");

        final int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        final int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        // Two samples per frame * 2 to support narration speeds down to 0.5
        final int bufferSizeBytes = SAMPLES_PER_CODEC_FRAME * 2 * 2 * channelCount;

        this.mSilenceInputBuffer = new byte[bufferSizeBytes];
        this.mSilenceOutputBuffer = new byte[bufferSizeBytes];
        mSilenceRemover = new SilenceRemover(sampleRate, channelCount);

        this.mSonicInputBuffer = new byte[bufferSizeBytes];
        this.mSonicOutputBuffer = new byte[bufferSizeBytes];
        this.mSonic = new Sonic(sampleRate, channelCount);
        this.mLastInternalBuffer = ByteBuffer.wrap(mSonicOutputBuffer, 0, 0);
        setSpeed(mSpeed);
    }
}