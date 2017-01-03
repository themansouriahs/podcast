package org.bottiger.podcast.service.Downloader.engines;

/**
 * Created by aplb on 03-01-2017.
 */

public interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done, long startTime);
}
