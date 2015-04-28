package org.bottiger.podcast.listeners;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.PlayerButtonView;

import android.app.DownloadManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

public class DownloadProgressObservable {

	/**
	 * How often the UI should refresh
	 */
	private static long REFRESH_INTERVAL = 50; // 16 ms => 60 fps
	//TimeUnit.MILLISECONDS.convert(1,TimeUnit.SECONDS);

	private static HashMap<IEpisode, List<DownloadObserver>> mObservers = new HashMap<>();
	private static DownloadManager mDownloadManager = null;
    private static HashMap<IEpisode, IDownloadEngine> mPodcastDownloadManager = null;

	/**
	 * Handler events types
	 */
	private static final int REFRESH_UI = 1;
    private static final int ADD_ID = 2;
    private static final int DELETED = 3;

    public DownloadProgressObservable(Context context) {
        mDownloadManager = (DownloadManager) context
                .getSystemService(Context.DOWNLOAD_SERVICE);
        mPodcastDownloadManager = EpisodeDownloadManager.mDownloadingEpisodes;
    }

	/**
	 * Handler for updating the textviews
	 */
	public static final Handler sHandler = new DownloadProgressHandler();

    private static class DownloadProgressHandler extends Handler {

        private List<FeedItem> mUpdateEpisodess = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        @Override
		public void handleMessage(Message msg) {

            // http://developer.android.com/reference/java/util/concurrent/locks/ReentrantLock.html
            lock.lock();  // block until condition holds
            try {

                switch (msg.what) {
                    case REFRESH_UI: {

                        Log.d("Refresh UI:", "Run at: "+ (new Date().getTime()));

                        Iterator<FeedItem> iterator = mUpdateEpisodess.iterator();
                        while (iterator.hasNext()) {

                            FeedItem episode = iterator.next();

                            if (episode == null) {
                                throw new NullPointerException("Episode can not be null!");
                            }

                            List<DownloadObserver> observers = mObservers.get(episode);

                            DownloadStatus status = EpisodeDownloadManager.getStatus(episode);
                            IDownloadEngine download = mPodcastDownloadManager.get(episode);

                            int progress = -1;

                            switch (status) {
                                case DOWNLOADING:
                                    if (download == null) {
                                        progress = episode.getProgress(mDownloadManager);
                                    } else {
                                        float progressFloat = download.getProgress();
                                        progress = (int)(progressFloat*100);
                                    }
                                    Log.d("Refresh UI:", "Downloading: "+ episode.title + " (progress: " + progress + ")");
                                    break;
                                case PENDING:
                                    Log.d("Refresh UI:", "Pending: "+ episode.title);
                                    break;
                                case DONE:
                                case NOTHING:
                                case ERROR:
                                    Log.d("Refresh UI:", "End: "+ episode.title);
                                    progress = 100;
                                    //mUpdateEpisodess.remove(episodeId);
                                    iterator.remove();
                                    break;
                                default:
                                    break;
                            }

                            if (progress >= 0 && observers != null) {
                                for (DownloadObserver observer : observers) {
                                    observer.setProgressPercent(progress);
                                }
                            }
                        }

                        if (!mUpdateEpisodess.isEmpty()) {
                            msg = sHandler.obtainMessage(REFRESH_UI);
                            sHandler.sendMessageDelayed(msg, REFRESH_INTERVAL);
                        }
                        break;
                    }
                    case ADD_ID: {
                        FeedItem episode = (FeedItem) msg.obj;
                        mUpdateEpisodess.add(episode);
                        refreshUI();
                        break;
                    }
                    case DELETED: {
                        FeedItem episode = (FeedItem) msg.obj;
                        List<DownloadObserver> observers = mObservers.get(episode.getId());
                        if (observers != null) {
                            for (DownloadObserver observer : observers) {
                                if (observer instanceof DownloadButtonView) {
                                    DownloadButtonView dbv = (DownloadButtonView) observer;
                                    dbv.setState(PlayerButtonView.STATE_DEFAULT);
                                }
                            }
                        }
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
        IEpisode observerFeedItem = argObserver.getEpisode();

        if (observerFeedItem == null) {
            Log.d("Warning", "observerFeedItem is null - this should never happen");
            return;
        }

        // Create a new list
        if (!mObservers.containsKey(observerFeedItem)) {
            mObservers.put(observerFeedItem, new LinkedList<DownloadObserver>());
        }

        mObservers.get(observerFeedItem).add(argObserver);

        refreshUI();
	}

	/**
	 * Unregister an Observer from being updated on progress updates
     *
     * Returns true if the observer was found and removed
	 */
	public boolean unregisterObserver(@NonNull DownloadObserver argObserver) {
        IEpisode observerFeedItem = argObserver.getEpisode();

        if (observerFeedItem == null)
            return false;

        if (!mObservers.containsKey(observerFeedItem)) {
            return false;
        }

        List<DownloadObserver> observers = mObservers.get(observerFeedItem);

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

    public void addEpisode(@NonNull FeedItem argEpisode) {
        Message msg = sHandler.obtainMessage(ADD_ID);
        msg.obj = argEpisode;
        sHandler.sendMessage(msg);
    }

    public static void deleteEpisode(@NonNull FeedItem argEpisode) {
        Message msg = sHandler.obtainMessage(DELETED);
        msg.obj = argEpisode;
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

}
