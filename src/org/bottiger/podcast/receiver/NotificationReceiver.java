package org.bottiger.podcast.receiver;

import java.util.List;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.service.PlayerService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class NotificationReceiver extends  BroadcastReceiver {

	public static final String toggleAction = SoundWaves.packageName + ".TOGGLE";
	public static final String nextAction = SoundWaves.packageName + ".NEXT";
	
	private PlayerService mPlayerServiceBinder = PodcastBaseFragment.mPlayerServiceBinder;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (intent.equals(toggleAction)) {
			if (mPlayerServiceBinder.isPlaying())
				mPlayerServiceBinder.pause();
			else
				mPlayerServiceBinder.start();
		}
		
		if (intent.equals(nextAction)) {
		} 
		
	}
	
}
