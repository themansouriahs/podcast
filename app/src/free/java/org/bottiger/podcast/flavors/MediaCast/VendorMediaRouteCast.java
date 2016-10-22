package org.bottiger.podcast.flavors.MediaCast;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouteSelector;
import android.view.Menu;

import com.google.android.exoplayer2.ExoPlayer;

import org.bottiger.podcast.MediaRouterPlaybackActivity;
import org.bottiger.podcast.flavors.player.googlecast.GoogleCastPlayer;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

import java.io.IOException;

/**
 * Created by apl on 12-04-2015.
 */
public class VendorMediaRouteCast extends GoogleCastPlayer implements IMediaCast {
    
    public VendorMediaRouteCast(MediaRouterPlaybackActivity mediaRouterPlaybackActivity) {
        super(mediaRouterPlaybackActivity);
    }

    @Override
    public void setupMediaButton(@NonNull Context argContext, Menu menu, @MenuRes int argMenuResource) {

    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean loadEpisode(IEpisode argEpisode) {
        return false;
    }

    @Override
    public void play(long argStartPosition) {

    }

    @Override
    public void pause() {

    }

    @Override
    public float getVolume() {
        return 0;
    }

    @Override
    public void setVolume(float vol) {

    }

    @Override
    public void addListener(ExoPlayer.EventListener listener) {

    }

    @Override
    public boolean isSteaming() {
        return false;
    }

    @Override
    public void stop() {

    }

    @Override
    public void removeListener(ExoPlayer.EventListener listener) {

    }

    @Override
    public void startAndFadeIn() {

    }

    @Override
    public void FaceOutAndStop(int argDelayMs) {

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
    public long seekTo(long msec) throws IllegalStateException {
        return 0;
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
    public void registerStateChangedListener(IMediaRouteStateListener argListener) {

    }

    @Override
    public void unregisterStateChangedListener(IMediaRouteStateListener argListener) {

    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    public void startDiscovery() {
    }

    public void stopDiscovery() {
    }

    public MediaRouteSelector getRouteSelector() {
        return null;
    }
}
