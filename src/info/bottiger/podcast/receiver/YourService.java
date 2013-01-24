package info.bottiger.podcast.receiver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/*
 * Example of how to trigger an automated update
 */
public class YourService extends Service
{
	PodcastUpdateManager alarm = new PodcastUpdateManager();
    public void onCreate()
    {
        super.onCreate();       
    }

    public void onStart(Context context,Intent intent, int startId)
    {
        alarm.setUpdate(context);
    }

    @Override
    public IBinder onBind(Intent intent) 
    {
        return null;
    }
}