package org.bottiger.podcast.receiver;

import org.bottiger.podcast.service.PodcastService;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class PowerReceiver extends BroadcastReceiver {

	private static final int POWERED_REFRESH_INTERVAL_MINUTES = 60;
	private static final int BATTERY_REFRESH_INTERVAL_MINUTES = 3 * POWERED_REFRESH_INTERVAL_MINUTES;
	
	private static final int POWERED_NEXT_REFRESH = 10;
	private static final int BATTERY_NEXT_REFRESH = 120;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		Intent alarmIntent = new Intent(context, PodcastService.class);
		int interval = -1;
		int nextRefresh = -1;

		if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
			// Do something when power connected
			interval = POWERED_REFRESH_INTERVAL_MINUTES;
			nextRefresh = POWERED_NEXT_REFRESH;
		} else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
			// Do something when power disconnected
			interval = BATTERY_REFRESH_INTERVAL_MINUTES;
			nextRefresh = BATTERY_NEXT_REFRESH;
		}

		if (interval > 0) {
			PendingIntent pendingAlarm = PodcastService.getAlarmIntent(context);
			PodcastService.setAlarm(context, pendingAlarm, nextRefresh, interval);
		}
	}

}
