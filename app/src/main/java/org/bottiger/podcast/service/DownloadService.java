package org.bottiger.podcast.service;

import android.app.IntentService;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.listeners.DownloadProgressPublisher;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.NetworkUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager.postQueueChangedEvent;

/**
 * Created by apl on 18-09-2014.
 */
public class DownloadService extends IntentService {

    private static final String SERVICE_NAME = "SoundWaves Download Service";
    private static final String TAG = DownloadService.class.getSimpleName();

    private static final String PARAM_IN_ID = "id";
    private static final String PARAM_IN_URL = "url";
    private static final String PARAM_IN_MANUAL_START = "started_manual";

    private static ReentrantLock sLock = new ReentrantLock();
    private static MutableLiveData<List<QueueEpisode>> sLiveQueue = new MutableLiveData<>();

    private DownloadProgressPublisher mProgressPublisher;
    private SoundWavesDownloadManager mSoundWavesDownloadManager;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public DownloadService() {
        super(SERVICE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSoundWavesDownloadManager = new SoundWavesDownloadManager(getApplicationContext());
        mProgressPublisher = new DownloadProgressPublisher(SoundWaves.getAppContext(getApplicationContext()), mSoundWavesDownloadManager);
    }

    /**
     *
     * @param argEpisode
     * @return true if the episode was queue for download
     */
    public static boolean download(@NonNull IEpisode argEpisode, boolean argStartedManually, @NonNull Context argContext) {
        Intent msgIntent = new Intent(argContext, DownloadService.class);

        if (argEpisode instanceof FeedItem) {
            long id = ((FeedItem) argEpisode).getId();
            msgIntent.putExtra(DownloadService.PARAM_IN_ID, id);
        }

        msgIntent.putExtra(DownloadService.PARAM_IN_URL, argEpisode.getURL());
        msgIntent.putExtra(DownloadService.PARAM_IN_MANUAL_START, argStartedManually);

        argContext.startService(msgIntent);

        return true;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        QueueEpisode episode = getEpisode(intent);

        if (episode != null) {
            try {
                sLock.lock();
                List<QueueEpisode> episodes = sLiveQueue.getValue();

                if (episodes == null)
                    episodes = new LinkedList<>();

                if (!episodes.contains(episode)) {
                    episodes.add(episode);

                    sLiveQueue.setValue(episodes);
                    postQueueChangedEvent(episode.getEpisode(), SoundWavesDownloadManager.ADDED);
                }
            } finally {
                sLock.unlock();
            }
        }

        // make sure you call the super class to ensure everything performs as expected
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent called");

        boolean downloadStarted = false;
        QueueEpisode episode = null;

        // Locate the next episode to be downloaded.
        try {
            sLock.lock();
            int queueChanced = 0;

            List<QueueEpisode> episodes = sLiveQueue.getValue();

            int size = episodes == null ? 0 : episodes.size();
            for (int i = 0; i < size && !downloadStarted; i++) {
                episode = episodes.get(i);

                if (episode != null) {
                    boolean canDownload = false;
                    try {
                        canDownload = NetworkUtils.canDownload(episode, this, sLock);
                    } catch (IOException e) {
                        ErrorUtils.handleException(e);
                    }
                    if (canDownload) {
                        downloadStarted = true;
                    } else {
                        // in case the download couldn't start
                        //sQueue.remove(episode);
                        episodes.remove(episode);
                        queueChanced++;

                        // This need to be more general and work for SlimEpisodes
                        if (!(episode.getEpisode() instanceof FeedItem)) {
                            ((FeedItem)episode.getEpisode()).downloadAborted();
                        }
                    }
                }
            }

            sLiveQueue.postValue(episodes);

            if (queueChanced > 0) {
                postQueueChangedEvent(null, SoundWavesDownloadManager.UNDEFINED);
            }

        } finally {
            sLock.unlock();
        }

        if (episode == null) {
            return;
        }

        IEpisode feedItem = episode.getEpisode();

        IDownloadEngine mEngine = NetworkUtils.newEngine(this, feedItem);
        mEngine.addCallback(mSoundWavesDownloadManager.getIDownloadEngineCallback());

        Log.d(TAG, "Start downloading: " + feedItem);
        List<QueueEpisode> episodes = sLiveQueue.getValue();
        int size = episodes != null ? episodes.size() : 0;
        mEngine.startDownload(size > 0);

        if (feedItem instanceof FeedItem) {
            mProgressPublisher.addEpisode((FeedItem) feedItem);
        }
    }

    @Nullable
    private QueueEpisode getEpisode(@Nullable Intent argIntent) {
        if (argIntent == null)
            return null;

        String url = argIntent.getStringExtra(PARAM_IN_URL);
        long id = argIntent.getLongExtra(PARAM_IN_ID, -1);
        boolean startedManually = argIntent.getBooleanExtra(PARAM_IN_MANUAL_START, false);

        IEpisode episode = null;
        if (id > 0) {
            episode = SoundWaves.getAppContext(this).getLibraryInstance().getEpisode(id);
        } else {
            try {
                episode = new SlimEpisode("", new URL(url), "", null);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        if (episode == null) {
            return null;
        }

        QueueEpisode queueEpisode = new QueueEpisode(episode);
        queueEpisode.setStartedManually(startedManually);

        return queueEpisode;
    }

    @Deprecated
    @NonNull
    public static List<QueueEpisode> getQueue() {
        List<QueueEpisode> episodes = sLiveQueue.getValue();

        if (episodes == null) {
            return new LinkedList<>();
        }

        return episodes;
    }

    @NonNull
    public static MutableLiveData<List<QueueEpisode>> getLiveQueue() {
        return sLiveQueue;
    }

    public static int getSize() {
        return getQueue().size();
    }

    public static void removeFirst() {

        // Queue have been cleared while downloading the current file
        sLock.lock();
        try {
            List<QueueEpisode> episodes = sLiveQueue.getValue();

            if (episodes != null && episodes.size() > 0) {
                episodes.remove(0);
            }
            sLiveQueue.postValue(episodes);
        } finally {
            sLock.unlock();
        }
    }

    public static boolean isRunning() {
        return getSize()>0;
    }

    /* Return the FeedItem currently being downloaded
     *
     * @return The downloading FeedItem
     */
     @Nullable
     public static IEpisode getDownloadingItem() {
         sLock.lock();
         try {
             List<QueueEpisode> episodes = sLiveQueue.getValue();

             if (episodes == null || episodes.size() == 0)
                 return null;

             QueueEpisode downloadingItem = episodes.get(0);

             if (downloadingItem == null) {
                 return null;
             }

             return downloadingItem.getEpisode();
         } finally {
             sLock.unlock();
         }
     }

    public static void clearQueue() {
        sLock.lock();
        try {
            List<QueueEpisode> episodes = sLiveQueue.getValue();

            if (episodes != null) {
                episodes.clear();
                sLiveQueue.postValue(episodes);
            }
        } finally {
            sLock.unlock();
        }
    }

    public static void removeFromQueue(int argPosition) {
        sLock.lock();
        try {
            List<QueueEpisode> episodes = sLiveQueue.getValue();
            if (episodes != null && episodes.size() > argPosition) {
                episodes.remove(argPosition);
                sLiveQueue.postValue(episodes);
            }
        } finally {
            sLock.unlock();
        }
    }

    public static void move(int fromPosition, int toPosition) {
        sLock.lock();
        try {
            List<QueueEpisode> episodes = sLiveQueue.getValue();

            if (episodes != null) {
                QueueEpisode episode = episodes.get(fromPosition);
                episodes.add(toPosition, episode);
                sLiveQueue.postValue(episodes);
            }
        } finally {
            sLock.unlock();
        }
    }

}
