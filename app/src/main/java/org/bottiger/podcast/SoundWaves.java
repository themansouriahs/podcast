package org.bottiger.podcast;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.player.sonic.service.ISoundWavesEngine;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.CrashReporterFactory;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.rxbus.RxBus;

public class SoundWaves extends Application {

    /*
    static {
        System.loadLibrary("hello-jni");
    }
    */

    public native String stringFromJNI();

    //public static PlayerService sBoundPlayerService = null;
    private static Context context;
    // Global constants
    private Boolean mFirstRun = null;

    public static IAnalytics sAnalytics;
    public static SubscriptionRefreshManager sSubscriptionRefreshManager;

    private static Bus sBus = new Bus(ThreadEnforcer.MAIN);
    private static RxBus _rxBus = null;

    private static Library sLibrary = null;

    public static class PlayerServiceBound {
        public boolean isConnected;
    }

    @Deprecated
    public static PlayerService sBoundPlayerService = null;

    public ISoundWavesEngine soundService;

    public ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("PlayerService", "onServiceConnected");
            sBoundPlayerService = ((PlayerService.PlayerBinder) service)
                        .getService();
            PlayerServiceBound playerServiceBound = new PlayerServiceBound();
            playerServiceBound.isConnected = true;
            sBus.post(playerServiceBound);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d("PlayerService", "onServiceDisconnected");
            sBoundPlayerService = null;
        }
    };

    public ServiceConnection soundEngineServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("SoundEngine", "onServiceConnected");
            soundService = ISoundWavesEngine.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d("SoundEngine", "onServiceDisconnected");
            soundService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        //if (!BuildConfig.DEBUG) { //  || System.currentTimeMillis() > 0
            // ACRA - crash reporter
            CrashReporterFactory.startReporter(this);

            // ANR
            //new ANRWatchDog(10000 /*timeout*/).start();
        //}

        sAnalytics = AnalyticsFactory.getAnalytics(this);
        sAnalytics.startTracking();

        context = getApplicationContext();

        sLibrary = new Library(this);

        sSubscriptionRefreshManager = new SubscriptionRefreshManager(context);

        firstRun(context);
    }

    public static Context getAppContext() {
        return context;
    }

    public void startService(){
        // Start the player service
        Intent serviceIntent = new Intent(this, PlayerService.class);
        startService(serviceIntent);
        Intent bindIntent = new Intent(this, PlayerService.class);
        bindService(bindIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);

        /*
        Intent soundServiceIntent = new Intent(this, SoundService.class);
        startService(soundServiceIntent);
        Intent soundBindIntent = new Intent(this, SoundService.class);
        bindService(soundBindIntent, soundEngineServiceConnection, Context.BIND_AUTO_CREATE);
        */
    }

    private void firstRun(@NonNull Context argContext) {
        SharedPreferences sharedPref = argContext.getSharedPreferences(ApplicationConfiguration.packageName, Context.MODE_PRIVATE);
        String key = getString(R.string.preference_first_run_key);
        boolean firstRun = sharedPref.getBoolean(key, true);
        if (firstRun) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, false);
            editor.commit();
        }
        mFirstRun = firstRun;
    }

    public boolean IsFirstRun() {
        if (mFirstRun == null) {
            throw new IllegalStateException("First run can not be null!");
        }

        return mFirstRun.booleanValue();
    }

    public static Bus getBus() {
        return sBus;
    }

    // This is better done with a DI Library like Dagger
    public static RxBus getRxBus() {
        if (_rxBus == null) {
            _rxBus = new RxBus();
        }

        return _rxBus;
    }

    public static Library getLibraryInstance() {
        if (sLibrary == null) {
            sLibrary = new Library(getAppContext());
        }

        return sLibrary;
    }
}
