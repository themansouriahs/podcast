package org.bottiger.podcast.service;

import org.bottiger.podcast.utils.Log;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public abstract class AbstractAlarmService extends IntentService {

	// from http://it-ride.blogspot.dk/2010/10/android-implementing-notification.html
	private WakeLock mWakeLock;
	private final String BASETAG = "wakelock";
	private final String TAG;
	
	private final Log log = Log.getLog(getClass());
	
	/**
	 * Do the actual work when the alarm fires
	 */
	abstract protected void fireAlarm();
	
	/**
	 * Schedules the next update in milliseconds from now
	 */
	abstract protected long nextUpdateMs();
	
	public AbstractAlarmService(String name) {
		super(name);
		this.TAG = this.BASETAG + ": " + name;
	}
	
	/**
	 * Creates and intent to be executed when the alarm goes of.
	 * 
	 * @param context
	 * @return The intent to be executed when the alarm fires
	 */
	private static PendingIntent getAlarmIntent(Context context) {
		Intent i = new Intent(context, CloudSyncService.class);
        Bundle bundle = new Bundle();
        bundle.putString("store", "Activity1");
        i.putExtra("b", bundle);
        return PendingIntent.getService(context, 0, i, 0);	
	}

	
	/**
	 * Schedules a database sync every X minutes. Where X is defined by the user in the settings.
	 * 
	 * @param context
	 */
	public void setAlarm(Context context) {
		// Refresh interval
        long milliSeconds = nextUpdateMs();
        
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getAlarmIntent(context);
        am.cancel(pi);
        // by my own convention, minutes <= 0 means notifications are disabled
        if (milliSeconds > 0) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + milliSeconds,
                milliSeconds, pi);
        }
	}

	private final IBinder binder = new AbstractAlarmBinder();

	public class AbstractAlarmBinder extends Binder {
		public AbstractAlarmService getService() {
			return AbstractAlarmService.this;
		}
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		log.debug("onLowMemory()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	handleIntent(intent);
    	
        return START_NOT_STICKY;
    }
	
    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
        log.debug("onDestroy()");
    }
	
	/**
     * This is where we initialize. We call this when onStart/onStartCommand is
     * called by the system. We won't do anything with the intent here, and you
     * probably won't, either.
     */
    private void handleIntent(Intent intent) {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        
        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getBackgroundDataSetting()) {
            stopSelf();
            return;
        }
        
        // do the actual work, in a separate thread
        //Bundle bb = intent.getExtras().getBundle("b");
        //String s = bb.getString("store");
        fireAlarm();
    }
    
    /**
     * Not sure if this is ever called
     */
	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
	}

}
