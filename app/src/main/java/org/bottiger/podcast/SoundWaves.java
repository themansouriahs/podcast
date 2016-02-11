package org.bottiger.podcast;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Debug;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.player.sonic.service.ISoundWavesEngine;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.CrashReporterFactory;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.rxbus.RxBus;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SoundWaves extends Application {

    private static final String TAG = "SoundWaves";

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

    @Nullable
    public static IAnalytics sAnalytics;
    public static SubscriptionRefreshManager sSubscriptionRefreshManager;
    private static SoundWavesDownloadManager sDownloadManager;

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
        Log.v(TAG, "time: " + System.currentTimeMillis());
        super.onCreate();
        //Debug.startMethodTracing("startup");
        PodcastLog.initFileLog(this);

        context = getApplicationContext();

        Log.v(TAG, "time: " + System.currentTimeMillis());

        CrashReporterFactory.startReporter(this);

        Observable.just(getAppContext()).subscribeOn(Schedulers.newThread()).subscribe(new Subscriber<Context>() {
            @Override
            public void onCompleted() {
                Log.v(TAG, "Analytics started");
            }

            @Override
            public void onError(Throwable e) {
                Log.wtf(TAG, e.getMessage());
            }

            @Override
            public void onNext(Context context) {
                sAnalytics = AnalyticsFactory.getAnalytics(context);
                sAnalytics.startTracking();
            }
        });

        Log.v(TAG, "time: " + System.currentTimeMillis());

        sDownloadManager = new SoundWavesDownloadManager(this);

        Log.v(TAG, "time: " + System.currentTimeMillis());
        sLibrary = new Library(this);
        Log.v(TAG, "time: " + System.currentTimeMillis());

        sSubscriptionRefreshManager = new SubscriptionRefreshManager(context);

        Log.v(TAG, "time: " + System.currentTimeMillis());
        firstRun(context);
        Log.v(TAG, "time: " + System.currentTimeMillis());
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
    }

    private void firstRun(@NonNull Context argContext) {
        SharedPreferences sharedPref = argContext.getSharedPreferences(ApplicationConfiguration.packageName, Context.MODE_PRIVATE);
        String key = getString(R.string.preference_first_run_key);
        boolean firstRun = sharedPref.getBoolean(key, true);
        if (firstRun) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, false);
            editor.apply();
        }
        mFirstRun = firstRun;
    }

    public boolean IsFirstRun() {
        if (mFirstRun == null) {
            throw new IllegalStateException("First run can not be null!");
        }

        return mFirstRun;
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

    @NonNull
    public static SoundWavesDownloadManager getDownloadManager() {
        return sDownloadManager;
    }

    public static Library getLibraryInstance() {
        if (sLibrary == null) {
            sLibrary = new Library(getAppContext());
        }

        return sLibrary;
    }


    private static Observable<Playlist> sPlaylistObservabel;

    @NonNull
    public static Observable<Playlist> getPlaylistObservabel() {
        if (sPlaylistObservabel == null) {
            sPlaylistObservabel = SoundWaves.getRxBus()
                    .toObserverable()
                    .ofType(Playlist.class)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }

        return sPlaylistObservabel;
    }
}
