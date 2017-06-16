package org.bottiger.podcast.player.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import org.bottiger.podcast.utils.HttpUtils;

/**
 * Created by aplb on 06-10-2016.
 */

public class ExoPlayerMediaSourceHelper {

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private Context mContext;
    private Handler mainHandler;
    private EventLogger eventLogger;

    private DataSource.Factory mediaDataSourceFactory;

    public ExoPlayerMediaSourceHelper(@NonNull Context argContext) {
        mContext = argContext;
        mainHandler = new Handler(argContext.getMainLooper());
        eventLogger = new EventLogger();

        String userAgent = HttpUtils.getUserAgent(mContext);

        // Default parameters, except allowCrossProtocolRedirects is true
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                userAgent,
                null /* listener */,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true /* allowCrossProtocolRedirects */
        );

        //mediaDataSourceFactory = new DefaultDataSourceFactory(mContext, userAgent);
        mediaDataSourceFactory = new DefaultDataSourceFactory(
                mContext,
                null /* listener */,
                httpDataSourceFactory
        );
    }

    @NonNull
    public MediaSource buildMediaSource(@NonNull Uri uri) {
        return buildMediaSource(uri, null);
    }

    @NonNull
    public MediaSource buildMediaSource(@NonNull Uri uri, @Nullable String overrideExtension) {
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(getBandwidthMeter(false)),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(getBandwidthMeter(false)),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case C.TYPE_OTHER: {
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(), mainHandler, eventLogger);
            }
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    @Nullable
    private DefaultBandwidthMeter getBandwidthMeter(boolean useBandwidthMeter) {
        return useBandwidthMeter ? BANDWIDTH_METER : null;
    }

    private DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(mContext, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(HttpUtils.getUserAgent(mContext), bandwidthMeter);
    }
}
