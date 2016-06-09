package org.bottiger.podcast.service.jobservice;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.util.Log;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by apl on 09-11-2014.
 */
@TargetApi(21)
public class PodcastUpdateJobService extends JobService {

    private static final String TAG = "PodcastUpdateJobService";

    private static final int HOURS = 48;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");

        SubscriptionRefreshManager subscriptionRefreshManager = new SubscriptionRefreshManager(this);
        subscriptionRefreshManager.refresh(null, new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean argSucces, @NonNull ISubscription argSubscription) {

                boolean downloadOnUpdate = PreferenceHelper.getBooleanPreferenceValue(getApplicationContext(),
                        R.string.pref_download_on_update_key,
                        R.bool.pref_download_on_update_default);

                // Override the default download setting if needed.
                if (argSubscription instanceof Subscription) {
                    Subscription subscription = (Subscription)argSubscription;
                    downloadOnUpdate = subscription.doDownloadNew(downloadOnUpdate);
                }

                if (!downloadOnUpdate)
                    return;

                SoundWavesDownloadManager downloadManager = SoundWaves.getAppContext(getApplicationContext()).getDownloadManager();

                SortedList<? extends IEpisode> episodes = argSubscription.getEpisodes();
                FeedItem episode;

                for (int i = 0; i < episodes.size(); i++) {
                    /**
                     * Currently we download an episode if:
                     *
                     * 1) We should download automatically (checked above)
                     * 2) It is less than 48 hours old
                     * 3) It has not been downloaded before
                     *
                     */
                    episode = (FeedItem)episodes.get(i);
                    Date episodeDate = episode.getDateTime();
                    if (episodeDate != null && episode.getCreatedAt().getTime() > 0) {
                        episodeDate = episode.getCreatedAt();
                    }

                    if (episodeDate != null) {
                        long createdAt = episodeDate.getTime();
                        long now = System.currentTimeMillis();
                        long hoursOld = TimeUnit.MILLISECONDS.toHours(now - createdAt);

                        if (hoursOld <= HOURS && !episode.hasBeenDownloadedOnce() && episode.isNew()) {
                            downloadManager.addItemToQueue(episode, false, SoundWavesDownloadManager.ANYWHERE);
                        }
                    } else {
                        Log.w(TAG, "EpisodeDate not set");
                    }
                }

                //SoundWavesDownloadManager.removeExpiredDownloadedPodcasts(PodcastUpdateJobService.this);

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
