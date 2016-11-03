package org.bottiger.podcast.widgets;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.AndroidUtil;
import org.bottiger.podcast.utils.StrUtils;

import static org.bottiger.podcast.notification.NotificationPlayer.REQUEST_CODE;

/**
 * Created by aplb on 26-05-2016.
 */

public class SoundWavesWidgetProvider extends AppWidgetProvider {

    // log tag
    private static final String TAG = "SWWidgetProvider";

    int mHeight = -1;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_default);

            attachButtonListeners(context, views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            //appWidgetManager.updateAppWidget(appWidgetId, views);
            updateAppWidget(context, appWidgetId, false);
        }
    }

    public static void updateAllWidgets(@NonNull Context context) {
        AppWidgetManager mgr= AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, SoundWavesWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        for (int i = 0; i < ids.length; i++) {
            updateAppWidget(context, ids[i], false);
        }
    }

    static void updateAppWidget(Context context, int appWidgetId, boolean showDescription) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        if (AndroidUtil.SDK_INT >= 16) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            showDescription = doShowDescription(options);
        }

        Playlist playlist = SoundWaves.getAppContext(context).getPlaylist();
        Library library = SoundWaves.getAppContext(context).getLibraryInstance();

        boolean wasEmpty = false;
        if (playlist.isEmpty()) {
            library.loadPlaylistSync(playlist);
            playlist.populatePlaylistIfEmpty();
            wasEmpty = true;
        }

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_default);

        boolean playlistEmpty = playlist.size() == 0;
        int emptyTextVisibility = playlistEmpty ? View.VISIBLE : View.GONE;
        int playerVisibility = !playlistEmpty ? View.VISIBLE : View.GONE;

        views.setViewVisibility(R.id.widget_player, playerVisibility);

        Log.wtf(TAG, "pre");

        IEpisode episode = playlist.first();

        if (episode != null) {

            Log.wtf(TAG, episode.getTitle());

            CharSequence podcast_title = "No title";
            if (episode.getSubscription(context) != null)
                podcast_title = episode.getSubscription(context).getTitle();

            //podcast_title =   (wasEmpty ? "Loaded" : "Memory") + podcast_title;

            CharSequence text = episode.getTitle();
            Long durationMs = episode.getDuration();
            Long elapsedTimeMs = episode.getOffset();
            boolean isPlaying = PlayerService.isPlaying();

            double progress = 0;
            if (durationMs > 0)
                progress = elapsedTimeMs * 100 / durationMs;

            views.setTextViewText(R.id.widget_title, podcast_title);
            views.setTextViewText(R.id.widget_episode_title, text);

            String chronometerFormat = "%s"; //episode.getOffset() + " / " + StrUtils.formatTime(durationMs);

            int playPauseIcon = !isPlaying ? R.drawable.ic_play_arrow_black : R.drawable.ic_pause_black;
            views.setImageViewResource(R.id.widget_play, playPauseIcon);

            views.setChronometer(R.id.widget_duration, SystemClock.elapsedRealtime() - elapsedTimeMs, chronometerFormat, isPlaying);
            views.setTextViewText(R.id.widget_duration_total, " / " + StrUtils.formatTime(durationMs));
            views.setProgressBar(R.id.progressBar, 100, (int) progress, false);
            views.setTextViewText(R.id.widget_description, episode.getDescription()); //

            int descriptionVisibility = showDescription ? View.VISIBLE : View.GONE;
            views.setViewVisibility(R.id.widget_description, descriptionVisibility);

            if (Build.VERSION.SDK_INT >= 23) {
                views.setViewVisibility(R.id.widget_mute, View.VISIBLE);

                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                boolean isMuted = audioManager.isStreamMute(PlayerStateManager.AUDIO_STREAM);
                int volumeIcon = isMuted ? R.drawable.ic_volume_off_24dp : R.drawable.ic_volume_up_24dp;
                views.setImageViewResource(R.id.widget_mute, volumeIcon);
            } else {
                views.setViewVisibility(R.id.widget_mute, View.GONE);
            }

            String imageUrl = episode.getArtwork(context);
            if (imageUrl != null) {
                AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, views, R.id.widget_logo, appWidgetId);

                Glide.with(context.getApplicationContext()) // safer!
                        .load(imageUrl)
                        .asBitmap()
                        .into(appWidgetTarget);
            }
        }

        attachButtonListeners(context, views);

        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    @TargetApi(16)
    public void onAppWidgetOptionsChanged(@NonNull Context context,
                                          @NonNull AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          @NonNull Bundle newOptions) {

        boolean showDescription = doShowDescription(newOptions);
        updateAppWidget(context, appWidgetId, showDescription);
    }

    @TargetApi(16)
    private static boolean doShowDescription(@NonNull Bundle newOptions) {
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH); // portrait
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT); // portrait

        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH); // landscape
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT); // landscape

        // 180 dp = 3 blocks: https://developer.android.com/guide/practices/ui_guidelines/widget_design.html#anatomy
        return maxHeight > 180;
    }

    private static void attachButtonListeners(Context context, RemoteViews views) {

            Intent toggleIntent = new Intent(context, SoundWavesWidgetProvider.class);
            toggleIntent.setAction(NotificationPlayer.toggleAction);

            Intent nextIntent = new Intent(context, SoundWavesWidgetProvider.class);
            nextIntent.setAction(NotificationPlayer.nextAction);

            Intent fastForwardIntent = new Intent(context, SoundWavesWidgetProvider.class);
            fastForwardIntent.setAction(NotificationPlayer.fastForwardAction);

            Intent rewindIntent = new Intent(context, SoundWavesWidgetProvider.class);
            rewindIntent.setAction(NotificationPlayer.rewindAction);

            Intent muteIntent = new Intent(context, SoundWavesWidgetProvider.class);
            muteIntent.setAction(NotificationPlayer.muteAction);

            PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, toggleIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent pendingNextIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent pendingFastForwardIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, fastForwardIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent pendingRewindIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, rewindIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent pendingMuteIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, muteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

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

}
