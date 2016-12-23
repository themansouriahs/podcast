package org.bottiger.podcast.service.jobservice;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.support.annotation.NonNull;
import android.util.Log;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.PreferenceHelper;

/**
 * Created by apl on 09-11-2014.
 */
@TargetApi(21)
public class PodcastUpdateJobService extends JobService {

    private static final String TAG = "PodcastUpdateJobService";

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "Job started");

        SubscriptionRefreshManager subscriptionRefreshManager = new SubscriptionRefreshManager(this);
        subscriptionRefreshManager.refresh(null, new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean argSucces, @NonNull ISubscription argSubscription) {

                boolean downloadOnUpdate = PreferenceHelper.getBooleanPreferenceValue(
                        getApplicationContext(),
                        R.string.pref_refresh_only_wifi_key,
                        R.bool.pref_refresh_only_wifi_default);

                // Override the default download setting if needed.
                if (argSubscription instanceof Subscription) {
                    Subscription subscription = (Subscription)argSubscription;
                    downloadOnUpdate = subscription.doDownloadNew(downloadOnUpdate);
                }

                if (!downloadOnUpdate)
                    return;

                SoundWavesDownloadManager.downloadNewEpisodes(getApplicationContext(), argSubscription);


                jobFinished(params, true);

            }
        });

        return true;
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
