package org.bottiger.podcast.service.jobservice;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;

/**
 * Created by apl on 09-11-2014.
 */
@TargetApi(21)
public class PodcastUpdateJobService extends JobService {

    private static final String TAG = "PodcastUpdateJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");

        SubscriptionRefreshManager subscriptionRefreshManager = new SubscriptionRefreshManager(this);
        subscriptionRefreshManager.refreshAll();
        EpisodeDownloadManager.removeExpiredDownloadedPodcasts(this);
        EpisodeDownloadManager.startDownload(this);

        return false;
    }

    /**
     * This method is called if the system has determined that you must stop
     * execution of your job even before you've had a chance to call jobFinished(JobParameters, boolean)
     *
     * @param params
     * @return True to indicate to the JobManager whether you'd like to reschedule this job based on the
     * retry criteria provided at job creation-time. False to drop the job. Regardless of the
     * value returned, your job must stop executing.
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped");

        return true;
    }
}
