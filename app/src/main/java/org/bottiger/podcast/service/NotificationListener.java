package org.bottiger.podcast.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Created by apl on 09-11-2014.
 */
@TargetApi(18)
public class NotificationListener extends NotificationListenerService {
    public NotificationListener() {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
