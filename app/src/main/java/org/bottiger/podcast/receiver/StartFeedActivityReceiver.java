package org.bottiger.podcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by aplb on 30-11-2016.
 */

public class StartFeedActivityReceiver extends BroadcastReceiver {

    private static final String TAG = StartFeedActivityReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "recieved");
        return;
    }
}
