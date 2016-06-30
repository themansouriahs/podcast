package org.bottiger.podcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.bottiger.podcast.service.jobservice.PodcastUpdater;

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

            // API 21 and above persists the JobService across reboots
        	if (Build.VERSION.SDK_INT < 21) {
                PodcastUpdater.setupAlarm(context);
            }
        }
    }
}