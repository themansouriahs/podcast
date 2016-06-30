package org.bottiger.podcast;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.multidex.MultiDexApplication;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import org.bottiger.podcast.cloud.EventLogger;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.player.SoundWavesPlayer;
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
import org.bottiger.podcast.service.jobservice.PodcastUpdater;
import org.bottiger.podcast.utils.PlayerHelper;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.rxbus.RxBus;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SoundWaves extends MultiDexApplication {

    private static final String TAG = "SoundWaves";

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    /*
    static {
        System.loadLibrary("hello-jni");
    }
    */

    public native String stringFromJNI();

    private static Context context;
    // Global constants
    private Boolean mFirstRun = null;

    @Nullable
    public static IAnalytics sAnalytics;
    private SubscriptionRefreshManager mSubscriptionRefreshManager;
    private SoundWavesDownloadManager mDownloadManager;
    private PodcastUpdater mPodcastUpdater;

    private static Bus sBus = new Bus(ThreadEnforcer.MAIN);
    private static RxBus _rxBus = null;

    private Library mLibrary = null;
    private Playlist mPlaylist = null;

    private MediaBrowserCompat mMediaBrowser;
    public MediaControllerCompat mMediaControllerCompat;
    PlayerHelper mPlayerHelper = new PlayerHelper();

    private GenericMediaPlayerInterface mPlayer;

    @Override
    public void onCreate() {
        Log.v(TAG, "App start time: " + System.currentTimeMillis());
        super.onCreate();
        //Debug.startMethodTracing("startup6");

        UIUtils.setTheme(getApplicationContext());

        if (BuildConfig.VERSION_CODE % 2 == 1)
            PodcastLog.initFileLog();

        context = getApplicationContext();

        Log.v(TAG, "time: " + System.currentTimeMillis());

        CrashReporterFactory.startReporter(this);

        Log.v(TAG, "time1: " + System.currentTimeMillis());

        Log.v(TAG, "time2: " + System.currentTimeMillis());
        //mLibrary = new Library(this);
        Log.v(TAG, "time3: " + System.currentTimeMillis());

        firstRun(context);
        Log.v(TAG, "time5: " + System.currentTimeMillis());

        initMediaBrowser();

        Log.v(TAG, "time6: " + System.currentTimeMillis());

        //mPlaylist = new Playlist(this);

        Log.v(TAG, "time7: " + System.currentTimeMillis());

        Log.v(TAG, "time8: " + System.currentTimeMillis());

        mPodcastUpdater = new PodcastUpdater(this);

        Log.v(TAG, "time9: " + System.currentTimeMillis());

        Observable.just(this)
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<Context>() {
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

                incrementStartupCount(context);
            }
        });
    }

    @Deprecated
    public static Context getAppContext() {
        return context;
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

    @WorkerThread
    private void incrementStartupCount(@NonNull Context argContext) {

        SharedPreferences sharedPref = argContext.getSharedPreferences(ApplicationConfiguration.packageName, Context.MODE_PRIVATE);
        String times_started_key = getString(R.string.pref_times_started);
        int times_started = sharedPref.getInt(times_started_key, 0) + 1;

        EventLogger.postEvent(argContext, EventLogger.START_APP, times_started, null, null);
        sharedPref.edit().putInt(times_started_key, times_started).apply();
    }

    public boolean IsFirstRun() {
        if (mFirstRun == null) {
            throw new IllegalStateException("First run can not be null!");
        }

        return mFirstRun;
    }

    @Deprecated
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
    public SoundWavesDownloadManager getDownloadManager() {
        if (mDownloadManager == null)  {
            mDownloadManager = new SoundWavesDownloadManager(this);
        }

        return mDownloadManager;
    }

    @NonNull
    public Library getLibraryInstance() {
        if (mLibrary == null) {
            mLibrary = new Library(this);
        }

        return mLibrary;
    }

    @NonNull
    public Playlist getPlaylist() {
        if (mPlaylist == null) {
            mPlaylist = new Playlist(this);
            getLibraryInstance().loadPlaylist(mPlaylist);
        }

        return mPlaylist;
    }

    @NonNull
    public GenericMediaPlayerInterface getPlayer() {
        if (mPlayer == null) {
            mPlayer = new SoundWavesPlayer(this);
        }

        return mPlayer;
    }

    public void setPlayer(@NonNull GenericMediaPlayerInterface argPlayer) {
        mPlayer = argPlayer;
    }

    @NonNull
    public static SoundWaves getAppContext(@NonNull Context argContext) {
        return (SoundWaves)argContext.getApplicationContext();
    }

    @NonNull
    public SubscriptionRefreshManager getRefreshManager() {
        if (mSubscriptionRefreshManager == null)
            mSubscriptionRefreshManager = new SubscriptionRefreshManager(this);

        return mSubscriptionRefreshManager;
    }

    private void initMediaBrowser() {
        // Start the player service
        mMediaBrowser = new MediaBrowserCompat(
                this, // a Context
                new ComponentName(this, PlayerService.class),
                // Which MediaBrowserService
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        try {
                            // Ah, here’s our Token again
                            MediaSessionCompat.Token token =
                                    mMediaBrowser.getSessionToken();
                            // This is what gives us access to everything
                            PlayerService ps = PlayerService.getInstance();

                            if (ps == null)
                                return;

                            SoundWaves.this.getPlayer().setPlayerService(ps);
                            mMediaControllerCompat =
                                    new MediaControllerCompat(SoundWaves.this, token);
                            mPlayerHelper.setMediaControllerCompat(mMediaControllerCompat);

                            // Convenience method of FragmentActivity to allow you to use
                            // getSupportMediaController() anywhere
                            //setSupportMediaController(mMediaControllerCompat);
                        } catch (RemoteException e) {
                            Log.e(MainActivity.class.getSimpleName(),
                                    "Error creating controller", e);
                            VendorCrashReporter.handleException(e);
                        }
                    }

                    @Override
                    public void onConnectionSuspended() {
                        // We were connected, but no longer :-(
                        VendorCrashReporter.report("onConnectionSuspended", "it happend");
                    }

                    @Override
                    public void onConnectionFailed() {
                        // The attempt to connect failed completely.
                        // Check the ComponentName!
                        VendorCrashReporter.report("onConnectionFailed", "it happend");
                    }
                },
                null); // optional Bundle
        mMediaBrowser.connect();
    }
}
