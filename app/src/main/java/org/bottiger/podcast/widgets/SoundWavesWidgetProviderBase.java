package org.bottiger.podcast.widgets;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.AppWidgetTarget;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.dependencyinjector.DependencyInjector;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.AndroidUtil;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.StrUtils;

import static org.bottiger.podcast.notification.NotificationPlayer.REQUEST_CODE;

/**
 * Created by aplb on 26-05-2016.
 */
abstract class SoundWavesWidgetProviderBase extends AppWidgetProvider {

    // log tag
    private static final String TAG = "SWWidgetProvider";

    @PlaybackStateCompat.State static int sState;

    static void updateAppWidget(Context context, int appWidgetId, boolean showDescription, int argLWidgetLayout) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

        int textVisibility = doShowCompactControls(options) ? View.VISIBLE : View.GONE;
        if (AndroidUtil.SDK_INT >= 16) {
            showDescription = doShowDescription(options);
        }

        Playlist playlist = SoundWaves.getAppContext(context).getPlaylist();
        Library library = SoundWaves.getAppContext(context).getLibraryInstance();

        if (playlist.isEmpty()) {
            library.loadPlaylistSync(playlist);
            playlist.populatePlaylistIfEmpty();
        }

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), argLWidgetLayout);

        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(context, 0, appIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_playlist_main, intent);

        boolean playlistEmpty = playlist.size() == 0;
        int emptyTextVisibility = playlistEmpty ? View.VISIBLE : View.GONE;
        int playerVisibility = !playlistEmpty ? View.VISIBLE : View.GONE;

        views.setViewVisibility(R.id.widget_player, playerVisibility);

        Log.wtf(TAG, "pre");

        IEpisode episode = playlist.first();

        if (episode != null) {

            Log.wtf(TAG, episode.getTitle());

            CharSequence podcast_title = "No title";
            podcast_title = episode.getSubscription(context).getTitle();

            CharSequence text = episode.getTitle();
            Long durationMs = episode.getDuration();
            Long elapsedTimeMs = episode.getOffset();
            boolean isPlaying = isPlaying();
            boolean isBuffering = isBuffering();

            double progress = 0;
            if (durationMs > 0)
                progress = elapsedTimeMs * 100 / durationMs;

            views.setTextViewText(R.id.widget_title, podcast_title);
            views.setTextViewText(R.id.widget_episode_title, text);

            String chronometerFormat = "%s"; //episode.getOffset() + " / " + StrUtils.formatTime(durationMs);

            int playPauseIcon = !(isPlaying || isBuffering) ? R.drawable.ic_play_arrow_black : R.drawable.ic_pause_black;
            views.setImageViewResource(R.id.widget_play, playPauseIcon);

            views.setChronometer(R.id.widget_duration, SystemClock.elapsedRealtime() - elapsedTimeMs, chronometerFormat, isPlaying);
            views.setTextViewText(R.id.widget_duration_total, " / " + StrUtils.formatTime(durationMs));
            views.setProgressBar(R.id.progressBar, 100, (int) progress, isBuffering);
            views.setTextViewText(R.id.widget_description, episode.getDescription()); //

            int descriptionVisibility = showDescription ? View.VISIBLE : View.GONE;
            views.setViewVisibility(R.id.widget_description, descriptionVisibility);

            // in case we are using the compact (one line) layout
            views.setViewVisibility(R.id.widget_title, textVisibility);
            views.setViewVisibility(R.id.widget_episode_title, textVisibility);
            views.setViewVisibility(R.id.widget_duration, textVisibility);
            views.setViewVisibility(R.id.widget_duration_total, textVisibility);

            if (Build.VERSION.SDK_INT >= 23) {
                views.setViewVisibility(R.id.widget_mute, textVisibility);

                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                boolean isMuted = audioManager.isStreamMute(PlayerStateManager.AUDIO_STREAM);
                int volumeIcon = isMuted ? R.drawable.ic_volume_off_24dp : R.drawable.ic_volume_up_24dp;
                views.setImageViewResource(R.id.widget_mute, volumeIcon);
            } else {
                views.setViewVisibility(R.id.widget_mute, View.GONE);
            }

            String imageUrl = episode.getArtwork(context);
            if (imageUrl != null) {
                AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, R.id.widget_logo, views, appWidgetId);

                // image size
                int imageSizeDp = (int) context.getResources().getDimension(R.dimen.widget_logo_size);
                //int imageSizePx = (int) UIUtils.convertDpToPixel(imageSizeDp, context);

                RequestOptions glideOptions = ImageLoaderUtils.getRequestOptions(context);
                glideOptions.override(imageSizeDp, imageSizeDp);

                RequestBuilder<Bitmap> builder = ImageLoaderUtils.getGlide(context, imageUrl);
                builder.apply(glideOptions);
                builder.into(appWidgetTarget);
            }
        }

        attachButtonListeners(context, views);

        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static boolean doShowCompactControls(@NonNull Bundle newOptions) {
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT); // portrait

        // 180 dp = 3 blocks: https://developer.android.com/guide/practices/ui_guidelines/widget_design.html#anatomy
        return maxHeight > 100;
    }

    @TargetApi(16)
    static boolean doShowDescription(@NonNull Bundle newOptions) {
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH); // portrait
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT); // portrait

        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH); // landscape
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT); // landscape

        // 180 dp = 3 blocks: https://developer.android.com/guide/practices/ui_guidelines/widget_design.html#anatomy
        return maxHeight > 180;
    }

    static void attachButtonListeners(Context context, RemoteViews views) {

        Class<?> pendingIntentReciever = PlayerService.class;

        Intent toggleIntent = new Intent(context, pendingIntentReciever);
        toggleIntent.setAction(NotificationPlayer.toggleAction);

        Intent nextIntent = new Intent(context, pendingIntentReciever);
        nextIntent.setAction(NotificationPlayer.nextAction);

        Intent fastForwardIntent = new Intent(context, pendingIntentReciever);
        fastForwardIntent.setAction(NotificationPlayer.fastForwardAction);

        Intent rewindIntent = new Intent(context, pendingIntentReciever);
        rewindIntent.setAction(NotificationPlayer.rewindAction);

        Intent muteIntent = new Intent(context, pendingIntentReciever);
        muteIntent.setAction(NotificationPlayer.muteAction);

        PendingIntent pendingToggleIntent = PendingIntent.getService(context, REQUEST_CODE, toggleIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingNextIntent = PendingIntent.getService(context, REQUEST_CODE, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingFastForwardIntent = PendingIntent.getService(context, REQUEST_CODE, fastForwardIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingRewindIntent = PendingIntent.getService(context, REQUEST_CODE, rewindIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingMuteIntent = PendingIntent.getService(context, REQUEST_CODE, muteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        views.setOnClickPendingIntent(R.id.widget_play, pendingToggleIntent);
        views.setOnClickPendingIntent(R.id.widget_skip_next, pendingNextIntent);
        views.setOnClickPendingIntent(R.id.widget_rewind, pendingRewindIntent);
        views.setOnClickPendingIntent(R.id.widget_fast_forward, pendingFastForwardIntent);
        views.setOnClickPendingIntent(R.id.widget_mute, pendingMuteIntent);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();

        MediaControllerCompat mediaControllerCompat = ((SoundWaves)ctx.getApplicationContext()).
                mMediaControllerCompat;

        if (mediaControllerCompat != null) {

            MediaControllerCompat.TransportControls transportControls = mediaControllerCompat.
                    getTransportControls();

            if (action.equals(NotificationPlayer.toggleAction)) {
                transportControls.sendCustomAction(PlayerStateManager.ACTION_TOGGLE, new Bundle());
            }
            if (action.equals(NotificationPlayer.muteAction)) {
                transportControls.sendCustomAction(PlayerStateManager.ACTION_TOGGLE_MUTE, new Bundle());
            }
            if (action.equals(NotificationPlayer.nextAction)) {
                transportControls.skipToNext();
            }
            if (action.equals(NotificationPlayer.fastForwardAction)) {
                transportControls.skipToNext();
            }
            if (action.equals(NotificationPlayer.rewindAction)) {
                transportControls.skipToNext();
            }

        }
        super.onReceive(ctx, intent);
    }

    int getWidgetLayout() {
        return R.layout.widget_default;
    }

    private static boolean isPlaying() {
        return sState == PlaybackStateCompat.STATE_PLAYING;
    }

    private static boolean isBuffering() {
        return sState == PlaybackStateCompat.STATE_BUFFERING;
    }

}
