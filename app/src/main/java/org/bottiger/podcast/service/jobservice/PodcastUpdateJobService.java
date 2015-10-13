package org.bottiger.podcast.service.jobservice;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.util.Log;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.PlayerService;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
        subscriptionRefreshManager.refresh(null, new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean argSucces, @NonNull ISubscription argSubscription) {

                if (argSubscription == null)
                    return;

                PlayerService ps = PlayerService.getInstance();
                SoundWavesDownloadManager downloadManager = null;
                if (ps != null)
                    downloadManager = ps.getDownloadManager();

                SortedList<? extends IEpisode> episodes = argSubscription.getEpisodes();
                IEpisode episode;
                if (episodes != null) {
                    for (int i = 0; i < episodes.size(); i++) {
                        episode = episodes.get(i);
                        long createdAt = episode.getCreatedAt().getTime();
                        long now = System.currentTimeMillis();
                        float buffer = 1.2f;

                        long minutes = TimeUnit.MILLISECONDS.toMinutes(now - createdAt);

                        if (minutes <= PodcastUpdater.UPDATE_FREQUENCY_MIN*buffer) {
                            if (downloadManager != null)
                                downloadManager.addItemToQueue(episode, SoundWavesDownloadManager.ANYWHERE);
                        }
                    }

                    SoundWavesDownloadManager.removeExpiredDownloadedPodcasts(PodcastUpdateJobService.this);
                    if (downloadManager != null)
                        downloadManager.startDownload();
                }
            }
        });

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
