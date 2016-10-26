package org.bottiger.podcast.service.jobservice;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.service.PodcastService;
import org.bottiger.podcast.utils.PreferenceHelper;

/**
 * Created by apl on 11-09-2014.
 */
public class PodcastUpdater implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final float UPDATE_FREQUENCY_MIN = 180f;

    private static final int PodcastUpdaterId = 36324;

    private Context mContext;

    public PodcastUpdater(@NonNull Context argContext) {

        mContext = argContext;
        PreferenceManager.getDefaultSharedPreferences(argContext).registerOnSharedPreferenceChangeListener(this);

        if (Build.VERSION.SDK_INT >= 21) {
            scheduleUpdateUsingJobScheduler(argContext);
        } else {
            setupAlarm(argContext);
        }

    }

    @TargetApi(21)
    private boolean scheduleUpdateUsingJobScheduler(@NonNull Context argContext) {

        if (Build.VERSION.SDK_INT < 21) {
            VendorCrashReporter.report("IllegalState", "scheduleUpdateUsingJobScheduler");
            return false;
        }

        ComponentName receiver = new ComponentName(argContext, PodcastUpdateJobService.class);
        boolean wifiOnly = PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_download_only_wifi_key,
                R.bool.pref_delete_when_finished_default);
        int networkType = wifiOnly ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY;
        long updateFrequencyMs = alarmInterval(UPDATE_FREQUENCY_MIN); // to ms

        JobInfo refreshFeedsTask = getJobInfo(receiver, networkType, updateFrequencyMs);

        JobScheduler jobScheduler =
                (JobScheduler) argContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        jobScheduler.cancel(PodcastUpdaterId);
        return jobScheduler.schedule(refreshFeedsTask) == JobScheduler.RESULT_SUCCESS;
    }

    @TargetApi(21)
    private JobInfo getJobInfo(@NonNull ComponentName argComponentName, int argNetworkType, long argUpdateFrequencyMs) {
        JobInfo refreshFeedsTask = new JobInfo.Builder(PodcastUpdaterId, argComponentName)
                .setRequiredNetworkType(argNetworkType)
                .setPersisted(true) // Persist across boots
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setPeriodic(argUpdateFrequencyMs)
                .build();

        return refreshFeedsTask;
    }

    /**
     * Creates and intent to be executed when the alarm goes of.
     *
     * @param context
     * @return The intent to be executed when the alarm fires
     */
    public static PendingIntent getAlarmIntent(Context context) {
        Intent i = new Intent(context, PodcastService.class);

        return PendingIntent.getService(context, 0, i, 0);
    }

    private static long nextAlarm(long minutes) {
        return SystemClock.elapsedRealtime() + alarmInterval(minutes);
    }

    private static long alarmInterval(float minutes) {
        return (long)(minutes * 60 * 1000); // minutes to milliseconds
    }


    /**
     * Setup a pending intent for the next alarm and schedules it
     *
     * @param context
     */
    public static void setupAlarm(Context context) {
        // Refresh interval
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        long minutes = prefs.getLong("interval", (long)UPDATE_FREQUENCY_MIN);

        PendingIntent pi = getAlarmIntent(context);
        setAlarm(context, pi, 15, minutes);
    }

    /**
     * Schedules a refresh every X minutes. Where X is defined by the user in
     * the settings.
     *
     * @param context
     */
    public static void setAlarm(Context context, PendingIntent pi,
                                long nextAlarmMinutes, long intervalMinutes) {
        AlarmManager am = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        // by my own convention, minutes <= 0 means notifications are disabled
        if (nextAlarmMinutes > 0) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    nextAlarm(nextAlarmMinutes),
                    alarmInterval(intervalMinutes), pi);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (Build.VERSION.SDK_INT < 21) {
            return;
        }

        String wifiKey = SoundWaves.getAppContext(mContext).getResources().getString(R.string.pref_download_only_wifi_key);

        if (wifiKey.equals(key)) {
            scheduleUpdateUsingJobScheduler(SoundWaves.getAppContext(mContext));
        }
    }
}
