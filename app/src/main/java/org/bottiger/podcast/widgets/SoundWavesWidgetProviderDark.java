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
public class SoundWavesWidgetProviderDark extends SoundWavesWidgetProvider {

    private static final int LAYOUT = R.layout.widget_default_dark;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), getWidgetLayout());

            attachButtonListeners(context.getApplicationContext(), views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            //appWidgetManager.updateAppWidget(appWidgetId, views);
            updateAppWidget(context.getApplicationContext(), appWidgetId, false, LAYOUT);
        }
    }

    public static void updateAllWidgets(@NonNull Context context, @PlaybackStateCompat.State int argState) {
        sState = argState;

        AppWidgetManager mgr= AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, SoundWavesWidgetProviderBase.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        for (int i = 0; i < ids.length; i++) {
            updateAppWidget(context, ids[i], false, LAYOUT);
        }
    }

    @Override
    @TargetApi(16)
    public void onAppWidgetOptionsChanged(@NonNull Context context,
                                          @NonNull AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          @NonNull Bundle newOptions) {

        boolean showDescription = doShowDescription(newOptions);
        updateAppWidget(context, appWidgetId, showDescription, LAYOUT);
    }
}
