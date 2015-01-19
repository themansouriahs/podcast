package org.bottiger.podcast.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.receiver.PodcastUpdateReceiver;
import org.bottiger.podcast.service.PodcastUpdateJobService;

/**
 * Created by apl on 11-09-2014.
 */
public class PodcastUpdater {

    public static final int PodcastUpdaterId = 36324;

    public static void scheduleUpdate(@NonNull Context argContext) {

        ComponentName receiver = new ComponentName(argContext, PodcastUpdateJobService.class);

        JobInfo uploadTask = new JobInfo.Builder(PodcastUpdaterId, receiver)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build();

        String test = Context.JOB_SCHEDULER_SERVICE;
        JobScheduler jobScheduler =
                (JobScheduler) argContext.getSystemService(test); // Context.JOB_SCHEDULER_SERVICE
        jobScheduler.schedule(uploadTask);
    }
}
