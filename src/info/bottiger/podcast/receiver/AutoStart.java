package info.bottiger.podcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver
{   
	PodcastUpdateManager alarm = new PodcastUpdateManager();
    @Override
    public void onReceive(Context context, Intent intent)
    {   
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            alarm.setUpdate(context);
        }
    }
}