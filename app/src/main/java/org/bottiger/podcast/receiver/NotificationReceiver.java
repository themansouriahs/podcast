package org.bottiger.podcast.receiver;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.Player.SoundWavesPlayer;
import org.bottiger.podcast.R;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

public class NotificationReceiver extends BroadcastReceiver {

	public static final String toggleAction = ApplicationConfiguration.packageName + ".TOGGLE";
	public static final String nextAction = ApplicationConfiguration.packageName + ".NEXT";
    public static final String clearAction = ApplicationConfiguration.packageName + ".CLEAR";

	private RemoteViews layout;
	private NotificationPlayer np;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		layout = new RemoteViews(context.getPackageName(), R.layout.notification);

        IBinder binder = peekService(context, new Intent(context, PlayerService.class));
        if (binder == null)
            return;
        PlayerService playerService = ((PlayerService.PlayerBinder) binder).getService();


        if (action.equals(nextAction)) {
            playerService.playNext();
            return;
        }

        if (action.equals(clearAction)) {
            playerService.halt();
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
                        np.show(isPlaying);
                    }

		}
		
	}
	
}
