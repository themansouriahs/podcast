package org.bottiger.podcast.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.StorageUtils;

public class PodcastUpdateReceiver extends BroadcastReceiver {
	
	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;
	private static final long ONE_DAY = 24L * ONE_HOUR;
	
	private static final long UPDATE_INTERVAL = ONE_MINUTE;
	private static SharedPreferences preferences;

	
    @Override
    public void onReceive(final Context context, Intent intent)
    {
        final long startTime = System.currentTimeMillis();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

		SubscriptionRefreshManager subscriptionRefreshManager = new SubscriptionRefreshManager(context);
        subscriptionRefreshManager.refresh(null, new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean succes, ISubscription argCallback) {
                final long endTime = System.currentTimeMillis();
                Integer timeDiff = (int) (endTime - startTime);
                IAnalytics analytics = AnalyticsFactory.getAnalytics(context);
                analytics.trackEvent(IAnalytics.EVENT_TYPE.REFRESH_DURATION, timeDiff);
                
                StorageUtils.removeExpiredDownloadedPodcasts(context);
                //downloadManager.startDownload();

                wl.release();
                return;
            }
        });
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