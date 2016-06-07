package org.bottiger.podcast.player.exoplayer;

/**
 * Created by aplb on 07-06-2016.
 */

public interface ProgressUpdateListener {
    void onProgressUpdate(long progress, long bufferedProgress, long duration);
}
