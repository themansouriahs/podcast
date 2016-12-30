package org.bottiger.podcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.notification.NewEpisodesNotification;

/**
 * Created by aplb on 30-12-2016.
 */

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.getAction());

        if (NewEpisodesNotification.notificationDeleteAction.equals(intent.getAction())) {
            SoundWaves.getAppContext(context).getLibraryInstance().clearNewEpisodeNotification();
            return;
        }

    }
}
