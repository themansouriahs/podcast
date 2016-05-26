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
import android.widget.RemoteViews;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;

import java.util.List;

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

    static void updateAppWidget(Context context, int appWidgetId, MediaMetadataCompat argMediaMetadataCompat) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        CharSequence text = argMediaMetadataCompat.getText(MediaMetadataCompat.METADATA_KEY_TITLE);
        Long durationMs = argMediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        String durationStr = Long.toString(durationMs);

        int progress = 50;

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the widget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_default);

        views.setTextViewText(R.id.widget_title, text);
        views.setTextViewText(R.id.widget_duration_total, durationStr);
        views.setChronometer(R.id.widget_duration, SystemClock.elapsedRealtime()-durationMs, "H:MM:SS", true);
        views.setProgressBar(R.id.progressBar, 1000, progress*10, false);

        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

}
