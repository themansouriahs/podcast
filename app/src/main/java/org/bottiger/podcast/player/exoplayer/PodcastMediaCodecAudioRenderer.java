package org.bottiger.podcast.player.exoplayer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.util.Util;

import org.bottiger.podcast.utils.PlaybackSpeed;

import java.nio.ByteBuffer;

/**
 * Created by aplb on 05-10-2016.
 */

public class PodcastMediaCodecAudioRenderer extends MediaCodecAudioRenderer {


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

    public PodcastMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector) {
        super(mediaCodecSelector);
    }

    public PodcastMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys) {
        super(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys);
    }

    public PodcastMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, Handler eventHandler, AudioRendererEventListener eventListener) {
        super(mediaCodecSelector, eventHandler, eventListener);
    }

    public PodcastMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, AudioRendererEventListener eventListener) {
        super(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener);
    }

    public PodcastMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities, int streamType) {
        super(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, audioCapabilities, streamType);
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
                                          ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs,
                                          boolean shouldSkip) throws ExoPlaybackException {

        if (DEBUG)
            Log.d(TAG, "processOutputBuffer");

        if (bufferIndex == mLastSeenBufferIndex) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec,
                    mLastInternalBuffer, bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        } else {
            mLastSeenBufferIndex = bufferIndex;
        }

        int bytesToRead;
        bytesToRead = buffer.remaining();

        buffer.get(mSonicInputBuffer, 0, bytesToRead);

        int bytesOld = bytesToRead;

        // Remove Silence
        //if (doRemoveSilence()) {
        if (false) {
            mSilenceOutputBuffer = mSilenceRemover.removeSilence(mSonicInputBuffer, mSilenceOutputBuffer, bytesToRead);
            bytesToRead = mSilenceRemover.getOutputLength();
        } else {
            mSilenceOutputBuffer = mSonicInputBuffer;
        }

        long us_skipped = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            if (bytesToRead != 0) {
                long us_pr_frame = (bufferPresentationTimeUs / bytesToRead);
                us_skipped = us_pr_frame * (bytesOld - bytesToRead);
            }
        }

        if (us_skipped > 0) {
            Log.w(TAG, "bytediff: " + (bytesOld - bytesToRead) + "time skipped: " + us_skipped);
        }

        positionUs += us_skipped;

        mSonic.writeBytesToStream(mSilenceOutputBuffer, bytesToRead);
        final int readThisTime = mSonic.readBytesFromStream(mSonicOutputBuffer, mSonicOutputBuffer.length);

        //bufferInfo.offset = 0;
        mLastInternalBuffer.position(0);

        //bufferInfo.size = readThisTime;
        mLastInternalBuffer.limit(readThisTime);

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, mLastInternalBuffer,
                bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);

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
