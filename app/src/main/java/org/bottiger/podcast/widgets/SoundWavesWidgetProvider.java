package org.bottiger.podcast.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
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
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.DateUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;

import java.util.List;

import static org.bottiger.podcast.R.id.chronometer;
import static org.bottiger.podcast.R.id.widget_duration;

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

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_default);
            views.setOnClickPendingIntent(R.id.widget_play, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    static void updateAppWidget(Context context, int appWidgetId) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        Library library = SoundWaves.getLibraryInstance();

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

            CharSequence podcast_title = episode.getSubscription().getTitle();
            CharSequence text = episode.getTitle();
            Long durationMs = episode.getDuration();
            boolean isPlaying = PlayerService.getInstance() != null && PlayerService.getInstance().isPlaying();

            double progress = episode.getProgress();

            views.setTextViewText(R.id.widget_title, podcast_title);
            views.setTextViewText(R.id.widget_episode_title, text);

            String chronometerFormat = "%s"; //episode.getOffset() + " / " + StrUtils.formatTime(durationMs);

            if (isPlaying) {
                //chronometerFormat = "%s / " + StrUtils.formatTime(durationMs);
            }

            int playPauseIcon = !isPlaying ? R.drawable.ic_play_arrow_black : R.drawable.ic_pause_black;
            views.setImageViewResource(R.id.widget_play, playPauseIcon);

            views.setChronometer(R.id.widget_duration, SystemClock.elapsedRealtime() - durationMs, chronometerFormat, true);
            views.setTextViewText(R.id.widget_duration_total, " / " + StrUtils.formatTime(durationMs));
            views.setProgressBar(R.id.progressBar, 100, (int) progress, false);

            String imageUrl = episode.getArtwork();
            if (imageUrl != null) {
                AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, views, R.id.widget_logo, appWidgetId);

                Glide
                        .with(context.getApplicationContext()) // safer!
                        .load(imageUrl)
                        .asBitmap()
                        .into(appWidgetTarget);
            }
        }

        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

}
