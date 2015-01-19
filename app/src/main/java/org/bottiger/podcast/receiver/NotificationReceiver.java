package org.bottiger.podcast.receiver;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.service.PlayerService;

import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class NotificationReceiver extends  BroadcastReceiver {

	public static final String toggleAction = SoundWaves.packageName + ".TOGGLE";
	public static final String nextAction = SoundWaves.packageName + ".NEXT";
	
	private PlayerService mPlayerServiceBinder = PodcastBaseFragment.mPlayerServiceBinder;
	private RemoteViews layout;
	private NotificationPlayer np;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		layout = new RemoteViews(context.getPackageName(), R.layout.notification);
		
		if (action.equals(toggleAction)) {
			Boolean isPlaying = false;
			if (mPlayerServiceBinder.isPlaying()) {
				mPlayerServiceBinder.pause();
			} else {
				mPlayerServiceBinder.start();
				isPlaying = true;
			}
			
			np = new NotificationPlayer(context, mPlayerServiceBinder.getCurrentItem());
			np.show(isPlaying, mPlayerServiceBinder);
		}
		
		if (action.equals(nextAction)) {
		} 
		
	}
	
}
