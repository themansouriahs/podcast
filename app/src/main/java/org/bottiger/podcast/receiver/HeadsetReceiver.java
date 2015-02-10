package org.bottiger.podcast.receiver;


import java.util.List;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.service.PlayerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

public class HeadsetReceiver extends BroadcastReceiver {

    private static List eventLog;
    private PlayerService mPlayerServiceBinder = PodcastBaseFragment.mPlayerServiceBinder;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("HeadsetReceiver", "Action: " + intent.getAction().toString() + "");

        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            mPlayerServiceBinder = PodcastBaseFragment.mPlayerServiceBinder;
            if (mPlayerServiceBinder != null) {
                if (mPlayerServiceBinder.isPlaying())
                    Log.d("HeadsetReceiver", "ACTION_AUDIO_BECOMING_NOISY => Pause Player");
                mPlayerServiceBinder.pause();
            }
        }

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event == null)
                return;

            Log.d("HeadsetReceiver", "Action: " + intent.getAction().toString() + " KeyCode: " + event.getKeyCode());
            if (KeyEvent.KEYCODE_HEADSETHOOK == event.getKeyCode()) {
                if (KeyEvent.ACTION_DOWN == event.getAction()) {
                    if (mPlayerServiceBinder.isPlaying()) {
                        Log.d("HeadsetReceiver", "ACTION_DOWN => Pause Player");
                        mPlayerServiceBinder.pause();
                    } else {
                        Log.d("HeadsetReceiver", "ACTION_DOWN => Start Player");
                        mPlayerServiceBinder.start();
                    }
                }
            }
        }
    }
}
