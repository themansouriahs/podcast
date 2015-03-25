package org.bottiger.podcast.listeners;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.IDownloadEngine;
import org.bottiger.podcast.service.PodcastDownloadManager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import com.google.api.client.util.DateTime;

public class DownloadProgressObservable {

	/**
	 * How often the UI should refresh
	 */
	private static long REFRESH_INTERVAL = 50; // 16 ms => 60 fps
	//TimeUnit.MILLISECONDS.convert(1,TimeUnit.SECONDS);

	private static WeakHashMap<Long, List<DownloadObserver>> mObservers = new WeakHashMap<Long, List<DownloadObserver>>();
	private static DownloadManager mDownloadManager = null;
    private static HashMap<Long, IDownloadEngine> mPodcastDownloadManager = null;

	/**
	 * Handler events types
	 */
	private static final int REFRESH_UI = 1;
    private static final int ADD_ID = 2;

    public DownloadProgressObservable(Context context) {
        mDownloadManager = (DownloadManager) context
                .getSystemService(Context.DOWNLOAD_SERVICE);
        mPodcastDownloadManager = PodcastDownloadManager.mDownloadingEpisodes;
    }

	/**
	 * Handler for updating the textviews
	 */
	public static final Handler sHandler = new Handler() {

        private List<Long> mUpdateIDs = new LinkedList<Long>();
        private final ReentrantLock lock = new ReentrantLock();

        @Override
		public void handleMessage(Message msg) {

            // http://developer.android.com/reference/java/util/concurrent/locks/ReentrantLock.html
            lock.lock();  // block until condition holds
            try {

                switch (msg.what) {
                    case REFRESH_UI: {

                        Log.d("Refresh UI:", "Run at: "+ (new Date().getTime()));

                        Iterator<Long> iterator = mUpdateIDs.iterator();
                        while (iterator.hasNext()) {
                            //Long episodeId : mUpdateIDs
                            Long episodeId = iterator.next();
                            List<DownloadObserver> observers = mObservers.get(episodeId);

                            if (observers == null || observers.isEmpty()) {
                                continue;
                            }

                            if (observers.get(0) == null) {
                                throw new IllegalStateException("huh");
                            }

                            FeedItem item = observers.get(0).getEpisode();

                            if (item == null) {
                                continue;
                            }

                            DownloadStatus status = PodcastDownloadManager.getStatus(item);
                            IDownloadEngine download = mPodcastDownloadManager.get(item.getId());

                            int progress = -1;

                            switch (status) {
                                case DOWNLOADING:
                                    Log.d("Refresh UI:", "Downloading: "+ item.title);
                                    if (download == null) {
                                        progress = item.getProgress(mDownloadManager);
                                    } else {
                                        float progressFloat = download.getProgress();
                                        progress = (int)(progressFloat*100);
                                    }
                                    break;
                                case PENDING:
                                    Log.d("Refresh UI:", "Pending: "+ item.title);
                                    break;
                                case DONE:
                                case NOTHING:
                                case ERROR:
                                    Log.d("Refresh UI:", "End: "+ item.title);
                                    progress = 100;
                                    //mUpdateIDs.remove(episodeId);
                                    iterator.remove();
                                    break;
                                default:
                                    break;
                            }

                            if (progress >= 0) {
                                for (DownloadObserver observer : observers) {
                                    observer.setProgressPercent(progress);
                                }
                            }
                        }

                        if (!mUpdateIDs.isEmpty()) {
                            msg = sHandler.obtainMessage(REFRESH_UI);
                            sHandler.sendMessageDelayed(msg, REFRESH_INTERVAL);
                        }
                        break;
                    }
                    case ADD_ID: {
                        Long episodeId = (Long) msg.obj;
                        mUpdateIDs.add(episodeId);
                        refreshUI();
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
	};

    /**
	 * Register an Observer to be updated on progress updates
	 *
	 */
	public void registerObserver(@NonNull DownloadObserver argObserver) {
        FeedItem observerFeedItem = argObserver.getEpisode();

        if (observerFeedItem == null) {
            Log.d("Warning", "observerFeedItem is null - this should never happen");
            return;
        }

        Long episodeId = observerFeedItem.getId();

        // Create a new list
        if (!mObservers.containsKey(episodeId)) {
            mObservers.put(episodeId, new LinkedList<DownloadObserver>());
        }

        mObservers.get(episodeId).add(argObserver);

        refreshUI();
	}

	/**
	 * Unregister an Observer from being updated on progress updates
     *
     * Returns true if the observer was found and removed
	 */
	public boolean unregisterObserver(@NonNull DownloadObserver argObserver) {
        FeedItem observerFeedItem = argObserver.getEpisode();

        if (observerFeedItem == null)
            return false;

        Long episodeId = observerFeedItem.getId();

        if (!mObservers.containsKey(episodeId)) {
            return false;
        }

        List<DownloadObserver> observers = mObservers.get(episodeId);

        // Just in case the GC was here in between. This should not be a problem anymore since we now have a strong reference.
        if (observers == null) {
            return false;
        }

       boolean isRemoved = observers.remove(argObserver);

		/*
		 * Stop the handler if the list is empty again
		 */
		if (mObservers.size() == 0) {
			sHandler.removeMessages(REFRESH_UI);
		}

		return isRemoved;
	}

    public void addEpisode(long argEpisodeId) {
        Message msg = sHandler.obtainMessage(ADD_ID);
        msg.obj = argEpisodeId;
        sHandler.sendMessage(msg);
    }

	/**
	 * Refrersh the UI handler
	 */
	private static void refreshUI() {
		sHandler.removeMessages(REFRESH_UI);
		Message msg = sHandler.obtainMessage(REFRESH_UI);
		sHandler.sendMessage(msg);
	}

    public static WeakHashMap<Long,List<DownloadObserver>> getObservers() {
        return mObservers;
    }

}
