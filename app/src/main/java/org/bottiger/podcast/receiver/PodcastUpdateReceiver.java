package org.bottiger.podcast.receiver;

import org.bottiger.podcast.service.PodcastDownloadManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class PodcastUpdateReceiver extends BroadcastReceiver {
	
	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;
	private static final long ONE_DAY = 24L * ONE_HOUR;
	
	private static final long UPDATE_INTERVAL = ONE_MINUTE;
	private static SharedPreferences preferences;

	
    @Override
    public void onReceive(Context context, Intent intent) 
    {   
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

		PodcastDownloadManager.start_update(context);
		PodcastDownloadManager.removeExpiredDownloadedPodcasts(context);
		PodcastDownloadManager.startDownload(context);

        wl.release();
    }

    public static void setUpdate(Context context)
    {
    	preferences =PreferenceManager
		.getDefaultSharedPreferences(context);
    	
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PodcastUpdateReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 10, pi); // Millisec * Second * Minute
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+UPDATE_INTERVAL,  pi);
    }

    public static void cancelUpdate(Context context)
    {
        Intent intent = new Intent(context, PodcastUpdateReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
    
    public static void updateNow(Context context) 
    {
    	cancelUpdate(context);
    	setUpdate(context);
    }   
}