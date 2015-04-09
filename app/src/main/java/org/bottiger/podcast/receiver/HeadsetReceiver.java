package org.bottiger.podcast.receiver;


import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.service.PlayerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

public class HeadsetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("HeadsetReceiver", "Action: " + intent.getAction().toString() + "");
        PlayerService playerService = MainActivity.getPlayerService();

        if (playerService == null) {
            Log.e("HeadsetReciever", "Warning, PlayerService is null");
            return;
        }

        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            if (playerService != null) {
                if (playerService.isPlaying())
                    Log.d("HeadsetReceiver", "ACTION_AUDIO_BECOMING_NOISY => Pause Player");
                playerService.pause();
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
                    if (playerService.isPlaying()) {
                        Log.d("HeadsetReceiver", "ACTION_DOWN => Pause Player");
                        playerService.pause();
                    } else {
                        Log.d("HeadsetReceiver", "ACTION_DOWN => Start Player");
                        playerService.start();
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    //context.startService(new Intent(MusicService.ACTION_PLAY));
                    playerService.start();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    //context.startService(new Intent(MusicService.ACTION_PAUSE));
                    playerService.pause();
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    //context.startService(new Intent(MusicService.ACTION_STOP));
                    playerService.stop();
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    //context.startService(new Intent(MusicService.ACTION_SKIP));
                    playerService.play(playerService.getNextId());
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
