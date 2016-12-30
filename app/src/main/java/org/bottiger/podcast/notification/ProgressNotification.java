package org.bottiger.podcast.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Formatter;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by aplb on 29-12-2016.
 */

public class ProgressNotification {

    private static final int sProgressNotificationId = 642;

    public static void show(@NonNull Context argContext, @NonNull IEpisode argEpisode, int progress, long argSpeedBps) {

        String speed = argSpeedBps > 0 ? Formatter.formatFileSize(argContext, argSpeedBps) + "/s" : "";

        NotificationCompat.Builder mBuilder = new NotificationCompat
                .Builder(argContext)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(argEpisode.getTitle())
                .setContentText(speed);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setCategory(Notification.CATEGORY_PROGRESS);
        }

        mBuilder.setProgress(100, progress, false);

        NotificationManager mNotificationManager =
                (NotificationManager) argContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(sProgressNotificationId, mBuilder.build());
    }

    public static void removeNotification(@NonNull Context argContext) {
        NotificationManager mNotificationManager =
                (NotificationManager) argContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.cancel(sProgressNotificationId);
    }

}
