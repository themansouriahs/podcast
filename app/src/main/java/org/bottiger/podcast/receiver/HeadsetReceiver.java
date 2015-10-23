package org.bottiger.podcast.receiver;


import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.service.PlayerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

            handleMediaButtonEvent(event, playerService);
        }
    }

    public static boolean handleMediaButtonEvent(@Nullable KeyEvent event, @NonNull PlayerService argPlayerService) {
        if (event == null)
            return false;

        if (event.getAction() != KeyEvent.ACTION_DOWN)
            return false;

        Log.d("HeadsetReceiver", "KeyCode: " + event.getKeyCode());

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                Log.d("HeadsetReceiver", "ACTION_DOWN => Toggle Player");
                argPlayerService.toggle();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                Log.d("HeadsetReceiver", "ACTION_DOWN => Play Player");
                argPlayerService.play();
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                Log.d("HeadsetReceiver", "ACTION_DOWN => Pause Player");
                argPlayerService.pause();
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                Log.d("HeadsetReceiver", "ACTION_DOWN => Stop Player");
                argPlayerService.stop();
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                // FIXME implement this
                argPlayerService.play(argPlayerService.getNextId().getUrl().toString());
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                // TODO: ensure that doing this in rapid succession actually plays the
                // previous song
                //context.startService(new Intent(MusicService.ACTION_REWIND));
                return false;
        }

        return true;
    }
}
