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
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Predicate;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;

import org.bottiger.podcast.utils.HttpUtils;

import java.io.IOException;

import okhttp3.OkHttpClient;

/**
 * A {@link RendererBuilder} for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements ExoPlayerWrapper.RendererBuilder {

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
  public Renderer[] buildRenderers(ExoPlayerWrapper player) {
      Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
      Handler mainHandler = player.getMainHandler();

      // Build the video and audio renderers.
      DefaultBandwidthMeter bandwidthMeter =
            new DefaultBandwidthMeter(mainHandler, null);

      OkHttpClient client = new OkHttpClient();
      Predicate<String> predicate = null; // FIXME: Optional, can this be done better?

      String useragent = HttpUtils.getUserAgent(context);
      OkHttpDataSource okHttpDataSource = new OkHttpDataSource(client, useragent, predicate);
      final HttpDataSource source = new OkHttpDataSource(client, useragent, predicate, bandwidthMeter);

      DataSource.Factory datasourceFactory = new DataSource.Factory() {
          @Override
          public DataSource createDataSource() {
              return source;
          }
      };

      ExtractorMediaSource.EventListener eventListener = new ExtractorMediaSource.EventListener() {
          @Override
          public void onLoadError(IOException error) {

          }
      };

      DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();

      MediaSource dataSource = new ExtractorMediaSource(uri, datasourceFactory, defaultExtractorsFactory, mainHandler, eventListener);

      //DataSource dataSource =
      //      new DefaultUriDataSource(context, bandwidthMeter, source);
      //ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
      //  BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE, mainHandler, player, 0);

      /*
      PodcastAudioRenderer podcastAudioRenderer = getAudioRenderer(dataSource);
      podcastAudioRenderer.setRemoveSilence(player.doRemoveSilence());

      // Invoke the callback.
      Renderer[] renderers = new Renderer[ExoPlayerWrapper.RENDERER_COUNT];
      renderers[ExoPlayerWrapper.TYPE_VIDEO] = getVideoRenderer(dataSource, mainHandler, player);
      renderers[ExoPlayerWrapper.TYPE_AUDIO] = podcastAudioRenderer;
      renderers[ExoPlayerWrapper.TYPE_TEXT] = getTextRenderer(dataSource, mainHandler, player);
      */

      //player.onRenderers(renderers, bandwidthMeter);

      return null; //renderers;
  }

    /*
    private TextRenderer getTextRenderer(@NonNull MediaSource sampleSource,
                                              @NonNull Handler mainHandler,
                                              @NonNull ExoPlayerWrapper player) {
        return new TextRenderer(sampleSource, player,
                mainHandler.getLooper());
    }

    @TargetApi(16)
    private MediaCodecVideoRenderer getVideoRenderer(@NonNull MediaSource sampleSource,
                                                     @NonNull Handler mainHandler,
                                                     @NonNull ExoPlayerWrapper player) {
        return new MediaCodecVideoRenderer(context,
                sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                mainHandler, player, 50);
    }

    private PodcastAudioRenderer getAudioRenderer(@NonNull MediaSource sampleSource) {
        return Util.SDK_INT >= 21 ? new PodcastAudioRendererV21(sampleSource) : new PodcastAudioRenderer(sampleSource);
    }
    */

    @Override
  public void cancel() {
    // Do nothing.
  }

}
