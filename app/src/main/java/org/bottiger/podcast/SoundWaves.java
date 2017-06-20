package org.bottiger.podcast;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.RemoteException;
import android.os.StrictMode;
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
import org.bottiger.podcast.dependencyinjector.DependencyInjector;
import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCodeFactory;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.NewPlayerEvent;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.jobservice.PodcastUpdater;
import org.bottiger.podcast.utils.PlayerHelper;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.rxbus.RxBus;
import org.bottiger.podcast.utils.rxbus.RxBus2;
import org.bottiger.podcast.utils.shortcuts.ShortcutManagerUtil;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import rx.Observable;
import rx.Subscriber;
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

    // Global constants
    private Boolean mFirstRun = null;

    @Nullable
    public static IAnalytics sAnalytics;
    private SubscriptionRefreshManager mSubscriptionRefreshManager;
    private SoundWavesDownloadManager mDownloadManager;
    private PodcastUpdater mPodcastUpdater;

    private static RxBus _rxBus = null;
    private static RxBus2 _rxBus2 = null;

    private Library mLibrary = null;
    private Playlist mPlaylist = null;

    private MediaBrowserCompat mMediaBrowser;
    public MediaControllerCompat mMediaControllerCompat;
    PlayerHelper mPlayerHelper = new PlayerHelper();

    FlowableProcessor<Integer> mChapterProcessor = BehaviorProcessor.create();

    private GenericMediaPlayerInterface mPlayer;

    @NonNull
    private PlayerStateManager mPlayerStateManager;

    @Override
    public void onCreate() {
        Log.v(TAG, "App start time: " + System.currentTimeMillis());
        super.onCreate();
        //Debug.startMethodTracing("startup6");

        enableStrictMode();
        injectDependencies();

        UIUtils.setTheme(getApplicationContext());

        if (BuildConfig.VERSION_CODE % 2 == 1)
            PodcastLog.initFileLog();

        Log.v(TAG, "time: " + System.currentTimeMillis());

        VendorCodeFactory.startReporter(this);

        VendorCodeFactory.startFirebase(this);

        Log.v(TAG, "time1: " + System.currentTimeMillis());

        firstRun(this);
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

        mPlayerStateManager = new PlayerStateManager();

        ShortcutManagerUtil.updateAppShortcuts(this, getPlaylist());
    }

    private void firstRun(@NonNull Context argContext) {
        SharedPreferences sharedPref = argContext.getSharedPreferences(ApplicationConfiguration.packageName, Context.MODE_PRIVATE);
        String key = getString(R.string.preference_first_run_key);
        String dateKet = getString(R.string.pref_app_first_start_date_key);
        boolean firstRun = sharedPref.getBoolean(key, true);

        if (firstRun) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, false);
            editor.putLong(dateKet, System.currentTimeMillis());
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

    @NonNull
    public IAnalytics getAnalystics() {
        return sAnalytics;
    }

    public boolean IsFirstRun() {
        if (mFirstRun == null) {
            throw new IllegalStateException("First run can not be null!");
        }

        return mFirstRun;
    }

    // This is better done with a DI Library like Dagger
    public static RxBus getRxBus() {
        if (_rxBus == null) {
            _rxBus = new RxBus();
        }

        return _rxBus;
    }

    public static RxBus2 getRxBus2() {
        if (_rxBus2 == null) {
            _rxBus2 = new RxBus2();
        }

        return _rxBus2;
    }

    @NonNull
    public SoundWavesDownloadManager getDownloadManager() {
        if (mDownloadManager == null)  {
            mDownloadManager = new SoundWavesDownloadManager(this);
        }

        return mDownloadManager;
    }

    @NonNull
    public PlayerStateManager getPlayerStateManager() {
        return mPlayerStateManager;
    }

    @NonNull
    public Library getLibraryInstance() {
        if (mLibrary == null) {
            mLibrary = new Library(this);
        }

        return mLibrary;
    }

    @NonNull
    public Playlist getPlaylist(boolean argPopulated) {
        if (mPlaylist == null) {
            mPlaylist = new Playlist(this);
        }

        if (argPopulated) {
            getLibraryInstance().loadPlaylistSync(mPlaylist);
            mPlaylist.populatePlaylistIfEmpty();
        }

        return mPlaylist;
    }

    @NonNull
    public Playlist getPlaylist() {
        return getPlaylist(false);
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
        mPlayer.setPlayerService(PlayerService.getInstance());

        NewPlayerEvent event = new NewPlayerEvent();
        getRxBus().send(event);
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

    @NonNull
    public FlowableProcessor<Integer> getChapterProcessor() {
        return mChapterProcessor;
    }

    public Flowable<Integer> getChapterObservable() {
        return mChapterProcessor
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void initMediaBrowser() {
        // Start the player service
        mMediaBrowser = new MediaBrowserCompat(
                this, // a Context
                new ComponentName(this, PlayerService.class),
                // Which MediaBrowserService
                getMediaBrowserCallback(),
                null); // optional Bundle
        mMediaBrowser.connect();
    }

    private void injectDependencies() {
        DependencyInjector.initialize(this);
        DependencyInjector.applicationComponent().inject(this);
    }

    private void enableStrictMode() {
        if (BuildConfig.DEBUG) {

            StrictMode.ThreadPolicy.Builder policy = new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .penaltyFlashScreen();
                    //.penaltyDeath() // also penaltyLog()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                policy.detectUnbufferedIo();
            }

            StrictMode.setThreadPolicy(policy
                    .build());

            StrictMode.VmPolicy.Builder vmpolicy = new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    //.detectLeakedClosableObjects() // problem with okhttp
                    .penaltyLog();
                    //.penaltyDeath()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vmpolicy.detectContentUriWithoutPermission()
                        .detectUntaggedSockets();
            }

            StrictMode.setVmPolicy(vmpolicy
                    .build());
        }
    }

    @NonNull
    private MediaBrowserCompat.ConnectionCallback getMediaBrowserCallback() {
        return new MediaBrowserCompat.ConnectionCallback() {
            @Override
            public void onConnected() {
                try {
                    // Ah, here’s our Token again
                    MediaSessionCompat.Token token =
                            mMediaBrowser.getSessionToken();

                    mMediaControllerCompat = new MediaControllerCompat(SoundWaves.this, token);
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
                mPlayerHelper.setMediaControllerCompat(null);
                super.onConnectionSuspended();
            }

            @Override
            public void onConnectionFailed() {
                // The attempt to connect failed completely.
                // Check the ComponentName!
                VendorCrashReporter.report("onConnectionFailed", "it happend");
                super.onConnectionFailed();
            }
        };
    }
}
