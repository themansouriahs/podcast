package org.bottiger.podcast.listeners;

import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by apl on 06-08-2014.
 */
public class DownloadFileObserver extends FileObserver {

    private static final long RATE_LIMIT_MS = 1000;

    public static final String LOG_TAG = DownloadFileObserver.class.getSimpleName();

    private boolean prematureGC = true;
    private final String mPath;

    private Date mLastUpdate;
    private Date mCurrentTime;
    private HashMap<String, Long> mEpisodeIdLUT = new HashMap<String, Long>();
    private final ReentrantLock mLock = new ReentrantLock();

    private DownloadProgressObservable mProgressObservable;

    private static final int flags =
            FileObserver.CLOSE_WRITE
                    | FileObserver.OPEN
                    | FileObserver.MODIFY
                    | FileObserver.DELETE
                    | FileObserver.MOVED_FROM;
    // Received three of these after the delete event while deleting a video through a separate file manager app:
    // 01-16 15:52:27.627: D/APP(4316): DownloadsObserver: onEvent(1073741856, null)

    public DownloadFileObserver(@NonNull String path, @NonNull long argEpisodeId, @NonNull DownloadProgressObservable argDownloadProgressObservable) {
        super(path, flags);

        mPath = path;

        try {
            mLock.lock();

            if (mEpisodeIdLUT.containsKey(path)) {
                throw new IllegalStateException("The LUT must not already contain the file");
            }

            mEpisodeIdLUT.put(path, argEpisodeId);

        } finally {
            mLock.unlock();
        }

        mProgressObservable = argDownloadProgressObservable;
        mLastUpdate = new Date();

        File file = new File(mPath);
        do {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!file.exists());

        this.startWatching();
    }


    @Override
    public void onEvent(int event, String path) {
        Log.d(LOG_TAG, "onEvent(" + event + ", " + path + ")");

        path = mPath;

        mCurrentTime = new Date();

        if (doWait(mCurrentTime, mLastUpdate)) {
            return;
        }

        long episode = mEpisodeIdLUT.get(path);
        switch (event) {
            case FileObserver.CLOSE_WRITE:
                // Download complete, or paused when wifi is disconnected. Possibly reported more than once in a row.
                // Useful for noticing when a download has been paused. For completions, register a receiver for
                // DownloadManager.ACTION_DOWNLOAD_COMPLETE.
                prematureGC = false;
                try {
                    mLock.lock();
                    /*
                    if (mEpisodeIdLUT.containsKey(path)) {
                        mEpisodeIdLUT.remove(path);
                    }*/
                    mProgressObservable.addEpisode(episode);
                } finally {
                    mLock.unlock();
                }
                break;
            case FileObserver.OPEN:
                // Called for both read and write modes.
                // Useful for noticing a download has been started or resumed.
                break;
            case FileObserver.DELETE:
            case FileObserver.MOVED_FROM:
                // These might come in handy for obvious reasons.
                break;
            case FileObserver.MODIFY:
                // Called very frequently while a download is ongoing (~1 per ms).
                // This could be used to trigger a progress update, but that should probably be done less often than this.
                try {
                    mLock.lock();

                    File file = new File(path);
                    if (file == null) {
                        break;
                    }

                    if (!mEpisodeIdLUT.containsKey(path)) {
                        break;
                    }

                    mProgressObservable.addEpisode(episode);
                } finally {
                    mLock.unlock();
                }

                break;
        }

        mLastUpdate = new Date();
    }

    protected void finalize() {
        prematureGC = true;
    }

    private boolean doWait(Date argCurrentTime, Date argLastUpdate) {
        long timeDiffMs = argCurrentTime.getTime() - argLastUpdate.getTime();

        return timeDiffMs < RATE_LIMIT_MS;
    }
}
