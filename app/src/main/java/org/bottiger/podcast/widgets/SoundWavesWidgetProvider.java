package org.bottiger.podcast.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.DateUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;

import java.util.List;

import static org.bottiger.podcast.R.id.chronometer;
import static org.bottiger.podcast.R.id.widget_duration;
import static org.bottiger.podcast.notification.NotificationPlayer.REQUEST_CODE;

/**
 * Created by aplb on 26-05-2016.
 */

public class SoundWavesWidgetProvider extends AppWidgetProvider {

    // log tag
    private static final String TAG = "SWWidgetProvider";

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
            updateAppWidget(context, appWidgetId);
        }
    }

    public static void updateAllWidgets(@NonNull Context context) {
        AppWidgetManager mgr= AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, SoundWavesWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        for (int i = 0; i < ids.length; i++) {
            updateAppWidget(context, ids[i]);
        }
    }

    static void updateAppWidget(Context context, int appWidgetId) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        Library library = SoundWaves.getAppContext(context).getLibraryInstance();
        library.loadPlaylistSync(SoundWaves.getAppContext(context).getPlaylist());

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_default);

        boolean playlistEmpty = library.getEpisodes().size() == 0;
        int emptyTextVisibility = playlistEmpty ? View.VISIBLE : View.GONE;
        int playerVisibility = !playlistEmpty ? View.VISIBLE : View.GONE;

        //views.setViewVisibility(R.id.widget_playlist_empty_text, emptyTextVisibility);
        views.setViewVisibility(R.id.widget_player, playerVisibility);

        Log.wtf(TAG, "pre");

        if (!playlistEmpty) {

            IEpisode episode = library.getEpisodes().get(0);

            Log.wtf(TAG, episode.getTitle());

            CharSequence podcast_title = episode.getSubscription(context).getTitle();
            CharSequence text = episode.getTitle();
            Long durationMs = episode.getDuration();
            Long elapsedTimeMs = episode.getOffset();
            boolean isPlaying = PlayerService.isPlaying();

            double progress = episode.getProgress();

            views.setTextViewText(R.id.widget_title, podcast_title);
            views.setTextViewText(R.id.widget_episode_title, text);

            String chronometerFormat = "%s"; //episode.getOffset() + " / " + StrUtils.formatTime(durationMs);

            int playPauseIcon = !isPlaying ? R.drawable.ic_play_arrow_black : R.drawable.ic_pause_black;
            views.setImageViewResource(R.id.widget_play, playPauseIcon);

            views.setChronometer(R.id.widget_duration, SystemClock.elapsedRealtime() - elapsedTimeMs, chronometerFormat, isPlaying);
            views.setTextViewText(R.id.widget_duration_total, " / " + StrUtils.formatTime(durationMs));
            views.setProgressBar(R.id.progressBar, 100, (int) progress, false);

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
            if (action.equals(NotificationPlayer.nextAction)) {
                transportControls.skipToNext();
            }
            if (action.equals(NotificationPlayer.fastForwardAction)) {
                transportControls.skipToNext();
            }
            if (action.equals(NotificationPlayer.rewindAction)) {
                transportControls.skipToNext();
            }
            if (action.equals(NotificationPlayer.muteAction)) {

            }

        }
        super.onReceive(ctx, intent);
    }

}
