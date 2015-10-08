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

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.CrashReporterFactory;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.PlayerService;

//Acra debugging
/*
@ReportsCrashes(
        // not used
        formUri = "https://acra.bottiger.org/acra-soundwaves/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = ApplicationConfiguration.formUriBasicAuthLogin, // optional
        formUriBasicAuthPassword = ApplicationConfiguration.formUriBasicAuthPassword, // optional
        disableSSLCertValidation = true,
        mode = ReportingInteractionMode.DIALOG,
        forceCloseDialogAfterToast=true,
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        socketTimeout = 10000)
        */
public class SoundWaves extends Application {

    static {
        System.loadLibrary("hello-jni");
    }

    public native String stringFromJNI();

    //public static PlayerService sBoundPlayerService = null;
    private static Context context;
    // Global constants
    private Boolean mFirstRun = null;

    public static IAnalytics sAnalytics;
    public static SubscriptionRefreshManager sSubscriptionRefreshManager;

    private static Bus sBus = new Bus(ThreadEnforcer.MAIN);

    public static class PlayerServiceBound {
        public boolean isConnected;
    }

    @Deprecated
    public static PlayerService sBoundPlayerService = null;

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
        Log.i("SoundWaves", "Connected");
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
}
