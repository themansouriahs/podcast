package org.bottiger.podcast.player;

import android.app.PendingIntent;
import android.media.RemoteControlClient;
import android.os.Looper;

/**
 * Created by apl on 11-02-2015.
 */
public class LegacyRemoteControlClient extends RemoteControlClient {
    public LegacyRemoteControlClient(PendingIntent mediaButtonIntent) {
        super(mediaButtonIntent);
    }

    public LegacyRemoteControlClient(PendingIntent mediaButtonIntent, Looper looper) {
        super(mediaButtonIntent, looper);
    }
}
