package org.bottiger.podcast.service;

import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Created by apl on 09-11-2014.
 */
public class PodcastUpdateJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {

        PodcastDownloadManager.start_update(this);
        PodcastDownloadManager.removeExpiredDownloadedPodcasts(this);
        PodcastDownloadManager.startDownload(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
