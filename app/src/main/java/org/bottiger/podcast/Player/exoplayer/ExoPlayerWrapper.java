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
import android.media.MediaCodec.CryptoException;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.view.Surface;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioTrack;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.utils.rxbus.RxBusSimpleEvents;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_IDLE;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. It can be prepared
 * with one of a number of {@link RendererBuilder} classes to suit different use cases (e.g. DASH,
 * SmoothStreaming and so on).
 */
public class ExoPlayerWrapper implements ExoPlayer.EventListener {

    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Builds renderers for playback.
         *
         * @param player The player for which renderers are being built. {@link org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper#onRenderers}
         *     should be invoked once the renderers have been built. If building fails,
         *     {@link org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper#onRenderersError} should be invoked.
         */
        Renderer[] buildRenderers(org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper player);
        /**
         * Cancels the current build operation, if there is one. Else does nothing.
         * <p>
         * A canceled build operation must not invoke {@link org.bottiger.podcast.player.exoplayer#onRenderers} or
         * {@link org.bottiger.podcast.player.exoplayer#onRenderersError} on the player, which may have been released.
         */
        void cancel();
    }

    /**
     * A listener for core events.
     */
    public interface Listener {
        void onStateChanged(boolean playWhenReady, @SoundWavesPlayerBase.PlayerState int playbackState);
        void onError(Exception e);
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RENDERER_COUNT, TYPE_VIDEO, TYPE_AUDIO, TYPE_TEXT, TYPE_METADATA})
    public @interface PlayerType {}
    public static final int RENDERER_COUNT = 4;
    public static final int TYPE_VIDEO = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_TEXT = 2;
    public static final int TYPE_METADATA = 3;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private final CopyOnWriteArrayList<Listener> listeners;

    private int rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private ExoPlayer player;
    private MappingTrackSelector trackSelector;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    public ExoPlayerWrapper(@NonNull Context argContext) {
        listeners = new CopyOnWriteArrayList<>();
        lastReportedPlaybackState = STATE_IDLE;

        initializePlayer(argContext);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        maybeReportPlayerState();
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
        onPlayerStateChanged(getPlayWhenReady(), getPlaybackState());
    }

    public void release() {
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        player.release();
    }

    @SuppressWarnings("ResourceType")
    public @SoundWavesPlayerBase.PlayerState
    int getPlaybackState() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILDING) {
            return SoundWavesPlayerBase.STATE_BUFFERING;
        }
        int playerState = player.getPlaybackState();
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT && playerState == STATE_IDLE) {
            // This is an edge case where the renderers are built, but are still being passed to the
            // player's playback thread.
            return SoundWavesPlayerBase.STATE_BUFFERING;
        }
        return playerState;
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        for (Listener listener : listeners) {
            listener.onError(exception);
        }
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    }


    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        maybeReportPlayerState();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    public ExoPlayer getPlayer() {
        return player;
    }

    private void maybeReportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        @SoundWavesPlayerBase.PlayerState int playbackState = getPlaybackState();
        if (lastReportedPlayWhenReady != playWhenReady || lastReportedPlaybackState != playbackState) {
            for (Listener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);
            }
            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
        }
    }

    private void initializePlayer(@NonNull Context argContext) {

        if (player == null) {
            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererModePrefer = EXTENSION_RENDERER_MODE_PREFER;

            RenderersFactory renderersFactory = new DefaultRenderersFactory(argContext);

            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, new DefaultLoadControl());
            player.addListener(this);

            player.setPlayWhenReady(false);
        }
    }

}
