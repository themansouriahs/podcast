package org.bottiger.podcast.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by apl on 18-09-2014.
 */
public class DownloadService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
