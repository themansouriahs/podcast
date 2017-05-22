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
        void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                float pixelWidthHeightRatio);
    }

    /**
     * A listener for internal errors.
     * <p>
     * These errors are not visible to the user, and hence this listener is provided for
     * informational purposes only. Note however that an internal error may cause a fatal
     * error if the player fails to recover. If this happens, {@link Listener#onError(Exception)}
     * will be invoked.
     */
    public interface InternalErrorListener {
        void onRendererInitializationError(Exception e);
        void onAudioTrackInitializationError(AudioTrack.InitializationException e);
        void onAudioTrackWriteError(AudioTrack.WriteException e);
        void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs);
        void onDecoderInitializationError(MediaCodecRenderer.DecoderInitializationException e);
        void onCryptoError(CryptoException e);
        void onLoadError(int sourceId, IOException e);
        void onDrmSessionManagerError(Exception e);
    }

    /**
     * A listener for debugging information.
     */
    public interface InfoListener {
        void onVideoFormatEnabled(Format format, int trigger, long mediaTimeMs);
        void onAudioFormatEnabled(Format format, int trigger, long mediaTimeMs);
        void onDroppedFrames(int count, long elapsed);
        void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate);
        void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                           long mediaStartTimeMs, long mediaEndTimeMs);
        void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                             long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs);
        void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                  long initializationDurationMs);
        //void onAvailableRangeChanged(int sourceId, TimeRange availableRange);
    }

    /**
     * A listener for receiving notifications of timed text.
     */
    public interface CaptionListener {
        void onCues(List<Cue> cues);
    }

    /**
     * A listener for receiving ID3 metadata parsed from the media stream.
     */
    public interface Id3MetadataListener {
        void onId3Metadata(List<Id3Frame> id3Frames);
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

    private final Handler mainHandler;
    private final CopyOnWriteArrayList<Listener> listeners;

    private RendererBuilder rendererBuilder;
    private Renderer[] mTrackRendere = new Renderer[ExoPlayerWrapper.RENDERER_COUNT];

    private int rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private Surface surface;
    private Renderer videoRenderer;
    private Format videoFormat;
    private int videoTrackToRestore;

    private ExoPlayer player;
    private MappingTrackSelector trackSelector;
    //private TrackSelectionHelper trackSelectionHelper;
    private DataSource.Factory mediaDataSourceFactory;
    private boolean playerNeedsSource;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private BandwidthMeter bandwidthMeter;
    private boolean backgrounded;

    private CaptionListener captionListener;
    private Id3MetadataListener id3MetadataListener;
    private InternalErrorListener internalErrorListener;
    private InfoListener infoListener;

    boolean mDoRemoveSilence = false;
    boolean mAutomaticGainControl = false;

    public ExoPlayerWrapper(@NonNull Context argContext) {
        //player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        //player.addListener(this);
        //playerControl = new PlayerControl(player);
        mainHandler = new Handler();
        listeners = new CopyOnWriteArrayList<>();
        lastReportedPlaybackState = STATE_IDLE;
        //rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        // Disable text initially.
        //player.setSelectedTrack(TYPE_TEXT, TRACK_DISABLED);

        initializePlayer(argContext);
    }

    public void setRenderBuilder(@NonNull  RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
    }

    //public PlayerControl getPlayerControl() {
    //    return playerControl;
    //}

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setInternalErrorListener(InternalErrorListener listener) {
        internalErrorListener = listener;
    }

    public void setInfoListener(InfoListener listener) {
        infoListener = listener;
    }

    public void setCaptionListener(CaptionListener listener) {
        captionListener = listener;
    }

    public void setMetadataListener(Id3MetadataListener listener) {
        id3MetadataListener = listener;
    }

    /*
    public void setSurface(Surface surface) {
        this.surface = surface;
        pushSurface(false);
    }

    public Surface getSurface() {
        return surface;
    }

    public void blockingClearSurface() {
        surface = null;
        pushSurface(true);
    }
    */

    /*
    public int getTrackCount(int type) {
        return player.getTrackCount(type);
    }

    public MediaFormat getTrackFormat(int type, int index) {
        return player.getTrackFormat(type, index);
    }

    public int getSelectedTrack(int type) {
        return player.getSelectedTrack(type);
    }

    public void setSelectedTrack(int type, int index) {
        player.setSelectedTrack(type, index);
        if (type == TYPE_TEXT && index < 0 && captionListener != null) {
            captionListener.onCues(Collections.<Cue>emptyList());
        }
    }
    */

    public boolean getBackgrounded() {
        return backgrounded;
    }

    /*
    public void setBackgrounded(boolean backgrounded) {
        if (this.backgrounded == backgrounded) {
            return;
        }
        this.backgrounded = backgrounded;
        if (backgrounded) {
            videoTrackToRestore = getSelectedTrack(TYPE_VIDEO);
            setSelectedTrack(TYPE_VIDEO, TRACK_DISABLED);
            blockingClearSurface();
        } else {
            setSelectedTrack(TYPE_VIDEO, videoTrackToRestore);
        }
    }
    */

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        rendererBuilder.cancel();
        videoFormat = null;
        videoRenderer = null;
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        maybeReportPlayerState();
        Renderer[] trackRenderers = rendererBuilder.buildRenderers(this);

        mTrackRendere[TYPE_VIDEO] = trackRenderers[TYPE_VIDEO];
        mTrackRendere[TYPE_AUDIO] = trackRenderers[TYPE_AUDIO];
        mTrackRendere[TYPE_TEXT] = trackRenderers[TYPE_TEXT];
    }

    public boolean doRemoveSilence() {
        return mDoRemoveSilence;
    }

    public boolean doAutomaticGainControl() {
        return mAutomaticGainControl;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void setAutomaticGainControl(boolean argAutomaticGainControl) {
        mAutomaticGainControl = argAutomaticGainControl;
        float speed = 1.0f;

        if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] != null) {

            Renderer audioTrackRenderer = mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO];

            if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] instanceof PodcastAudioRenderer) {
                ((PodcastAudioRenderer) audioTrackRenderer).setAutomaticGainControl(argAutomaticGainControl);
            }

            // FIXME: not robust
            if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] instanceof PodcastAudioRendererV21) {
                speed = ((PodcastAudioRendererV21) audioTrackRenderer).getSpeed();
            }
        }

        notifyAudioEngineChange(speed);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void setRemoveSilence(boolean argDoRemoveSilence) {
        mDoRemoveSilence = argDoRemoveSilence;
        float speed = 1.0f;

        if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] != null) {

            Renderer audioTrackRenderer = mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO];

            if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] instanceof PodcastAudioRenderer) {
                ((PodcastAudioRenderer) audioTrackRenderer).setRemoveSilence(argDoRemoveSilence);
            }

            // FIXME: not robust
            if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] instanceof PodcastAudioRendererV21) {
                speed = ((PodcastAudioRendererV21) audioTrackRenderer).getSpeed();
            }
        }

        notifyAudioEngineChange(speed);
    }

    /*
    @TargetApi(Build.VERSION_CODES.M)
    public void setPlaybackSpeed(float argNewSpeed) {
        if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] != null) {

            if (mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO] instanceof PodcastAudioRendererV21) {
                ((PodcastAudioRendererV21) mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO]).setSpeed(argNewSpeed);
            } else {
                player.sendMessage(mTrackRendere[ExoPlayerWrapper.TYPE_AUDIO],
                        MediaCodecAudioRenderer.MSG_SET_PLAYBACK_PARAMS,
                        new PlaybackParams().setSpeed(argNewSpeed));
            }
        }

        notifyAudioEngineChange(argNewSpeed);
    }
    */

    private void notifyAudioEngineChange(float argSpeed) {
        SoundWaves.getRxBus().send(new RxBusSimpleEvents.PlaybackEngineChanged(argSpeed, doRemoveSilence(), doAutomaticGainControl()));
    }

    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param renderers Renderers indexed by {@link org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper} TYPE_* constants. An individual
     *     element may be null if there do not exist tracks of the corresponding type.
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth. May be null.
     */
  /* package */
    /*
    void onRenderers(Renderer[] renderers, BandwidthMeter bandwidthMeter) {
        for (int i = 0; i < RENDERER_COUNT; i++) {
            if (renderers[i] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[i] = new DummyTrackRenderer();
            }
        }
        // Complete preparation.
        this.videoRenderer = renderers[TYPE_VIDEO];
        this.codecCounters = videoRenderer instanceof MediaCodecRenderer
                ? ((MediaCodecRenderer) videoRenderer).codecCounters
                : renderers[TYPE_AUDIO] instanceof MediaCodecRenderer
                ? ((MediaCodecRenderer) renderers[TYPE_AUDIO]).codecCounters : null;
        this.bandwidthMeter = bandwidthMeter;
        pushSurface(false);
        player.prepare(renderers);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }
    */

    /**
     * Invoked if a {@link RendererBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
  /* package */ void onRenderersError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(e);
        }
        for (Listener listener : listeners) {
            listener.onError(e);
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        maybeReportPlayerState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
        onPlayerStateChanged(getPlayWhenReady(), getPlaybackState());
    }

    public void release() {
        rendererBuilder.cancel();
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        surface = null;
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

    /*
    @Override
    public Format getFormat() {
        return videoFormat;
    }

    @Override
    public BandwidthMeter getBandwidthMeter() {
        return bandwidthMeter;
    }
    */

    /*
    @Override
    public CodecCounters getCodecCounters() {
        return codecCounters;
    }
    */

    /*
    @Override
    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }
    */

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }


    /*Looper getPlaybackLooper() {
        return player.getPlaybackLooper();
    }
    *

    /* package */ Handler getMainHandler() {
        return mainHandler;
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

    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                              long mediaStartTimeMs, long mediaEndTimeMs) {
        if (infoListener != null) {
            infoListener.onLoadStarted(sourceId, length, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs);
        }
    }

    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                                long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (infoListener != null) {
            infoListener.onLoadCompleted(sourceId, bytesLoaded, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs);
        }
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

    /*
    private void pushSurface(boolean blockForSurfacePush) {
        if (videoRenderer == null) {
            return;
        }

        if (blockForSurfacePush) {
            player.blockingSendMessage(
                    videoRenderer, MediaCodecVideoRenderer.MSG_SET_SURFACE, surface);
        } else {
            player.sendMessage(
                    videoRenderer, MediaCodecVideoRenderer.MSG_SET_SURFACE, surface);
        }
    }
    */

    private void initializePlayer(@NonNull Context argContext) {

        if (player == null) {
            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererModePrefer = EXTENSION_RENDERER_MODE_PREFER;

            //eventLogger = new EventLogger();
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            //trackSelector.addListener(this);
            //trackSelector.addListener(eventLogger);
            //trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
            player = ExoPlayerFactory.newSimpleInstance(argContext, trackSelector, new DefaultLoadControl(),
                    null, extensionRendererModePrefer);
            player.addListener(this);
            //player.addListener(eventLogger);
            //player.setAudioDebugListener(eventLogger);
            //player.setVideoDebugListener(eventLogger);
            //player.setId3Output(eventLogger);
            //simpleExoPlayerView.setPlayer(player);

            //if (shouldRestorePosition) {
            //    if (playerPosition == C.TIME_UNSET) {
            //        player.seekToDefaultPosition(playerWindow);
            //    } else {
            //        player.seekTo(playerWindow, playerPosition);
            //    }
            //}

            player.setPlayWhenReady(false);
            playerNeedsSource = true;
        }

        if (playerNeedsSource) {

            Uri[] uris;
            String[] extensions;

            //if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
                // The player will be reinitialized if the permission is granted.
            //    return;
            //}

            /*
            MediaSource[] mediaSources = new MediaSource[uris.length];
            for (int i = 0; i < uris.length; i++) {
                mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
            }
            MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
                    : new ConcatenatingMediaSource(mediaSources);
            player.prepare(mediaSource, !shouldRestorePosition);
            playerNeedsSource = false;
            */
            // updateButtonVisibilities();
        }
    }

}
