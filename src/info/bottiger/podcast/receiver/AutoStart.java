package info.bottiger.podcast.receiver;

import info.bottiger.podcast.service.PodcastService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class AutoStart extends BroadcastReceiver
{   
	PodcastUpdateReceiver alarm = new PodcastUpdateReceiver();
    @Override
    public void onReceive(Context context, Intent intent)
    {   
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            //PodcastUpdateReceiver.setUpdate(context);
            // in our case intent will always be BOOT_COMPLETED, so we can just set
            // the alarm
            // Note that a BroadcastReceiver is *NOT* a Context. Thus, we can't use
            // "this" whenever we need to pass a reference to the current context.
            // Thankfully, Android will supply a valid Context as the first parameter
            
        	
        	PodcastService.setAlarm(context);
        }
    }
}