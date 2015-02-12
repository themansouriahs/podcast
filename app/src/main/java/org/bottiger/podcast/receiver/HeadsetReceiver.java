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
        mPlayerServiceBinder = PodcastBaseFragment.mPlayerServiceBinder;

        if (mPlayerServiceBinder == null) {
            Log.e("HeadsetReciever", "Warning, PlayerService is null");
            throw new IllegalStateException("PlayerService is null");
        }

        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            if (mPlayerServiceBinder != null) {
                if (mPlayerServiceBinder.isPlaying())
                    Log.d("HeadsetReceiver", "ACTION_AUDIO_BECOMING_NOISY => Pause Player");
                mPlayerServiceBinder.pause();
            }
        }

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event == null)
                return;

            if (event.getAction() != KeyEvent.ACTION_DOWN)
                return;

            Log.d("HeadsetReceiver", "Action: " + intent.getAction().toString() + " KeyCode: " + event.getKeyCode());

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    //context.startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
                    if (mPlayerServiceBinder.isPlaying()) {
                        Log.d("HeadsetReceiver", "ACTION_DOWN => Pause Player");
                        mPlayerServiceBinder.pause();
                    } else {
                        Log.d("HeadsetReceiver", "ACTION_DOWN => Start Player");
                        mPlayerServiceBinder.start();
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    //context.startService(new Intent(MusicService.ACTION_PLAY));
                    mPlayerServiceBinder.start();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    //context.startService(new Intent(MusicService.ACTION_PAUSE));
                    mPlayerServiceBinder.pause();
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    //context.startService(new Intent(MusicService.ACTION_STOP));
                    mPlayerServiceBinder.stop();
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    //context.startService(new Intent(MusicService.ACTION_SKIP));
                    mPlayerServiceBinder.play(mPlayerServiceBinder.getNextId());
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    // TODO: ensure that doing this in rapid succession actually plays the
                    // previous song
                    //context.startService(new Intent(MusicService.ACTION_REWIND));
                    break;
            }
        }
    }
}
