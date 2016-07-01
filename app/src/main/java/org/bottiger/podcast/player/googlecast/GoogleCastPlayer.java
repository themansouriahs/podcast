package org.bottiger.podcast.player.googlecast;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.flavors.MediaCast.IMediaRouteStateListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.Player;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import java.io.IOException;

/**
 * Created by aplb on 28-06-2016.
 */

public class GoogleCastPlayer extends SoundWavesPlayerBase implements IMediaRouteStateListener {

    // GoogleCast
    private IMediaCast mMediaCast;

    public GoogleCastPlayer(@NonNull IMediaCast argMediaCast, @NonNull Context argContext) {
        super(argContext);
        mMediaCast = argMediaCast;
    }

    public void setDataSourceAsync(String path, int startPos) {
        super.setDataSourceAsync(path, startPos);

        mMediaCast.loadEpisode(PlayerService.getCurrentItem());
        start();
        mPlayerStateManager.updateState(PlaybackStateCompat.STATE_PLAYING, startPos, getCurrentSpeedMultiplier());
    }

    @Override
    public boolean isSteaming() {
        return true;
    }

    public void start() {
        super.start();
        mMediaCast.play(0);
    }

    public void stop() {
        if (!isInitialized())
            return;

        mMediaCast.stop();

        super.stop();
    }

    @Override
    public boolean isPlaying() {
        return mMediaCast != null && mMediaCast.isPlaying();
    }

    public void pause() {
        mMediaCast.stop();
    }

    @Override
    public long duration() {
        return 0;
    }

    @Override
    public long position() {
        return mMediaCast.getCurrentPosition();
    }

    @Override
    public long getCurrentPosition() {
        return mMediaCast != null && mMediaCast.isActive() ? mMediaCast.getCurrentPosition() : 0;
    }

    @Override
    public void rewind(@Nullable IEpisode argItem) {
    }

    @Override
    public void fastForward(@Nullable IEpisode argItem) {

    }

    public long seek(long whereto) {
        mMediaCast.seekTo(whereto);

        return whereto;
    }

    @Override
    public void setVolume(float vol) {

    }

    @Override
    public void onStateChanged(IMediaCast castProvider) {
        if (castProvider == null)
            return;

        mMediaCast = castProvider;

        if (mPlayerService == null)
            return;

        IEpisode episode = mPlayerService.getCurrentItem();

        if (episode == null)
            return;

        if (mMediaCast.isConnected()) {
            mMediaCast.loadEpisode(episode);

            if (episode instanceof FeedItem) {
                mMediaCast.seekTo(((FeedItem)episode).offset);
            }

            if (isPlaying()) {
                long offst = getCurrentPosition();
                pause();
                mMediaCast.play(offst);
            }

            mStatus = PlayerStatusObservable.PLAYING;
            mPlayerService.notifyStatusChanged();
        }
    }

    @Override
    public boolean isCasting() {
        return mMediaCast != null && mMediaCast.isConnected();
    }

    @Override
    public boolean canSetPitch() {
        return false;
    }

    @Override
    public boolean canSetSpeed() {
        return false;
    }

    @Override
    public float getCurrentPitchStepsAdjustment() {
        return 0;
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public float getMaxSpeedMultiplier() {
        return 0;
    }

    @Override
    public float getMinSpeedMultiplier() {
        return 0;
    }

    @Override
    public void prepare() throws IllegalStateException, IOException {

    }

    @Override
    public void reset() {

    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {

    }

    @Override
    public void setAudioStreamType(int streamtype) {

    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, IllegalStateException, IOException {

    }

    @Override
    public void setPlaybackSpeed(float f) {

    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {

    }

    public void setMediaCast(IMediaCast mMediaCast) {
        this.mMediaCast = mMediaCast;
        mMediaCast.registerStateChangedListener(this);
    }

}
