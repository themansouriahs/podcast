/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bottiger.podcast.player.exoplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;

import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper.RendererBuilder;
import org.bottiger.podcast.utils.HttpUtils;

import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.Util;

import okhttp3.OkHttpClient;

/**
 * A {@link RendererBuilder} for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final Uri uri;

    public ExtractorRendererBuilder(Context context, Uri uri) {
      this.context = context;
      this.uri = uri;
    }

  @TargetApi(16)
  @Override
  public TrackRenderer[] buildRenderers(ExoPlayerWrapper player) {
      Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
      Handler mainHandler = player.getMainHandler();

      // Build the video and audio renderers.
      DefaultBandwidthMeter bandwidthMeter =
            new DefaultBandwidthMeter(mainHandler, null);

      OkHttpClient client = new OkHttpClient();
      UriDataSource source = new OkHttpDataSource(client, HttpUtils.getUserAgent(context), null, bandwidthMeter);

      DataSource dataSource =
            new DefaultUriDataSource(context, bandwidthMeter, source);
      ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
        BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE, mainHandler, player, 0);

      // Invoke the callback.
      TrackRenderer[] renderers = new TrackRenderer[ExoPlayerWrapper.RENDERER_COUNT];
      renderers[ExoPlayerWrapper.TYPE_VIDEO] = getVideoRenderer(sampleSource, mainHandler, player);
      renderers[ExoPlayerWrapper.TYPE_AUDIO] = getAudioRenderer(sampleSource);
      renderers[ExoPlayerWrapper.TYPE_TEXT] = getTextRenderer(sampleSource, mainHandler, player);
      player.onRenderers(renderers, bandwidthMeter);

      return renderers;
  }

    private TextTrackRenderer getTextRenderer(@NonNull ExtractorSampleSource sampleSource,
                                              @NonNull Handler mainHandler,
                                              @NonNull ExoPlayerWrapper player) {
        return new TextTrackRenderer(sampleSource, player,
                mainHandler.getLooper());
    }

    @TargetApi(16)
    private MediaCodecVideoTrackRenderer getVideoRenderer(@NonNull ExtractorSampleSource sampleSource,
                                                          @NonNull Handler mainHandler,
                                                          @NonNull ExoPlayerWrapper player) {
        return new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                mainHandler, player, 50);
    }

    private MediaCodecAudioTrackRenderer getAudioRenderer(@NonNull ExtractorSampleSource sampleSource) {
        return Util.SDK_INT >= 21 ? new PodcastAudioRendererV21(sampleSource) : new PodcastAudioRenderer(sampleSource);
    }

    @Override
  public void cancel() {
    // Do nothing.
  }

}
