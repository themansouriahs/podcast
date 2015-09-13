package org.bottiger.podcast.service.jobservice;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;

/**
 * Created by apl on 09-11-2014.
 */
@TargetApi(21)
public class PodcastUpdateJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {

        SubscriptionRefreshManager subscriptionRefreshManager = new SubscriptionRefreshManager(this);
        subscriptionRefreshManager.refreshAll();
        EpisodeDownloadManager.removeExpiredDownloadedPodcasts(this);
        EpisodeDownloadManager.startDownload(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
