package org.bottiger.podcast.service;

import android.app.IntentService;
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
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.utils.NetworkUtils;

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
    private static LinkedList<QueueEpisode> sQueue = new LinkedList<>();

    private DownloadProgressPublisher mProgressPublisher;
    private SoundWavesDownloadManager mSoundWavesDownloadManager;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DownloadService() {
        super(SERVICE_NAME);
        mSoundWavesDownloadManager = new SoundWavesDownloadManager(this);
        mProgressPublisher = new DownloadProgressPublisher((SoundWaves) SoundWaves.getAppContext(), mSoundWavesDownloadManager);
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
                sQueue.add(episode);
                postQueueChangedEvent();
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
            boolean queueChanced = false;

            for (int i = 0; i < sQueue.size() && !downloadStarted; i++) {
                episode = sQueue.getFirst();

                if (episode != null) {
                    boolean canDownload = NetworkUtils.canDownload(episode, this, sLock);
                    if (canDownload) {
                        downloadStarted = true;
                    } else {
                        // in case the download couldn't start
                        sQueue.remove(episode);
                        queueChanced = true;

                        // This need to be more general and work for SlimEpisodes
                        if (!(episode.getEpisode() instanceof FeedItem)) {
                            ((FeedItem)episode.getEpisode()).downloadAborted();
                        }
                    }
                }
            }

            if (queueChanced) {
                postQueueChangedEvent();
            }

        } finally {
            sLock.unlock();
        }

        if (episode == null) {
            return;
        }

        FeedItem feedItem = (FeedItem) episode.getEpisode();

        IDownloadEngine mEngine = NetworkUtils.newEngine(feedItem);
        mEngine.addCallback(mSoundWavesDownloadManager.getIDownloadEngineCallback());

        Log.d(TAG, "Start downloading: " + feedItem);
        mEngine.startDownload();
        mProgressPublisher.addEpisode(feedItem);
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
            episode = SoundWaves.getLibraryInstance().getEpisode(id);
        }

        QueueEpisode queueEpisode = new QueueEpisode(episode);
        queueEpisode.setStartedManually(startedManually);

        return queueEpisode;
    }

    @NonNull
    public static List<QueueEpisode> getQueue() {
        return sQueue;
    }

    public static int getSize() {
        return sQueue.size();
    }

    public static void removeFirst() {
        sQueue.removeFirst();
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
             if (sQueue.size() == 0)
                 return null;

             QueueEpisode downloadingItem = sQueue.getFirst();
             if (downloadingItem == null) {
                 return null;
             }

             return downloadingItem.getEpisode();
         } finally {
             sLock.unlock();
         }
     }

    public static void clearQueue() {
        sQueue.clear();
    }

    public static void removeFromQueue(int argPosition) {
        sQueue.remove(argPosition);
    }

    public static void move(int fromPosition, int toPosition) {
        sLock.lock();
        try {
            QueueEpisode episode = sQueue.get(fromPosition);
            sQueue.add(toPosition, episode);
        } finally {
            sLock.unlock();
        }
    }
}
