package org.bottiger.podcast.receiver;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

	public static final String toggleAction = ApplicationConfiguration.packageName + ".TOGGLE";
	public static final String nextAction = ApplicationConfiguration.packageName + ".NEXT";
    public static final String clearAction = ApplicationConfiguration.packageName + ".CLEAR";
    public static final String playAction = ApplicationConfiguration.packageName + ".PLAY";
    public static final String pauseAction = ApplicationConfiguration.packageName + ".PAUSE";
    public static final String fastForwardAction = ApplicationConfiguration.packageName + ".FAST_FORWARD";
    public static final String rewindAction = ApplicationConfiguration.packageName + ".REWIND";

	private NotificationPlayer np;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();

        IBinder binder = peekService(context, new Intent(context, PlayerService.class));
        if (binder == null) {
            ((SoundWaves)context.getApplicationContext()).startService();
            return;
        }

        PlayerService playerService = ((PlayerService.PlayerBinder) binder).getService();

        executeCommand(playerService, action);
	}

    private void executeCommand(@NonNull PlayerService playerService, @NonNull String action) {
        if (action.equals(nextAction)) {
            playerService.playNext();
            return;
        }

        if (action.equals(clearAction)) {
            playerService.halt();
            return;
        }

        if (action.equals(playAction)) {
            playerService.play();
            return;
        }

        if (action.equals(pauseAction)) {
            playerService.pause();
            return;
        }

        if (action.equals(toggleAction)) {


            SoundWavesPlayer player = playerService.getPlayer();
            if (!player.isInitialized()) {
                return;
            }

            Boolean isPlaying = false;
            if (playerService.isPlaying()) {
                playerService.pause();
            } else {
                playerService.play();
                isPlaying = true;
            }

            IEpisode currentItem = playerService.getCurrentItem();
            if (currentItem != null) {
                np = new NotificationPlayer(playerService, playerService.getCurrentItem());
                np.setPlayerService(playerService);
                np.show(isPlaying, currentItem);
            }

        }
    }
	
}
