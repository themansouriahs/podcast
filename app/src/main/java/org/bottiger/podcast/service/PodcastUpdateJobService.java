package org.bottiger.podcast.service;

import android.app.job.JobParameters;
import android.app.job.JobService;

import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;

/**
 * Created by apl on 09-11-2014.
 */
public class PodcastUpdateJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {

        SubscriptionRefreshManager.start_update(this);
        EpisodeDownloadManager.removeExpiredDownloadedPodcasts(this);
        EpisodeDownloadManager.startDownload(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
