package org.bottiger.podcast.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.FragmentContainerActivity;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.StrUtils;

import java.util.LinkedList;
import java.util.List;

import static org.bottiger.podcast.notification.NotificationPlayer.REQUEST_CODE;

/**
 * Created by aplb on 30-12-2016.
 */

public class NewEpisodesNotification {

    public static final String notificationDeleteAction = ApplicationConfiguration.packageName + ".NOTIFICATION_DELETED";
    public static final String notificationOpenSubscriptionsAction = ApplicationConfiguration.packageName + ".NOTIFICATION_OPEN_SUBSCRIPTIONS";

    private static final long TIMEOUT = 86_400_000; // 24 h in ms

    private static final int sProgressNotificationId = 643;
    private static final int MAX_DISPLAYED_EPISODES = 5;
    private static final String GROUP_KEY_NEW_EPISODES = "group_new_episodes";

    private List<IEpisode> mEpisodes = new LinkedList<>();
    private List<ISubscription> mSubscriptions = new LinkedList<>();

    public synchronized void show(@NonNull Context argContext, @NonNull List<? extends IEpisode> argEpisodes) {

        NotificationChannels.INSTANCE.getSubscriptionUpdatedChannel(argContext);

        IEpisode episode = null;
        boolean addedEpisode = false;
        for (int i = 0; i < argEpisodes.size(); i++) {
            episode = argEpisodes.get(i);
            if (showEpisode(episode, argContext)) {
                mEpisodes.add(episode);
                ISubscription subscription = episode.getSubscription(argContext);
                if (!mSubscriptions.contains(subscription)) {
                    mSubscriptions.add(subscription);
                }
                addedEpisode = true;
            }
        }

        if (!addedEpisode) {
            return;
        }

        int episodeCount = mEpisodes.size();

        Resources resources = argContext.getResources();
        String title = resources.getQuantityString(R.plurals.notification_new_episodes_title, episodeCount, episodeCount);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        int displayedEpisodes = Math.min(episodeCount, MAX_DISPLAYED_EPISODES);
        int notDisplayedEpisodes = episodeCount-displayedEpisodes;

        for (int i = 0; i < displayedEpisodes; i++) {
            Spanned spanned = getLine(mEpisodes.get(i), argContext);
            inboxStyle.addLine(spanned);
        }

        inboxStyle.setBigContentTitle(title);
        if (notDisplayedEpisodes > 0) {
            String summary = resources.getQuantityString(R.plurals.notification_new_episodes_summary, notDisplayedEpisodes, notDisplayedEpisodes);
            inboxStyle.setSummaryText(summary);
        }

        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_sw);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(argContext, NotificationChannels.CHANNEL_ID_SUBSCRIPTION)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.soundwaves)
                        .setLargeIcon(bitmap)
                        .setContentInfo(String.valueOf(episodeCount))
                        .setAutoCancel(true)
                        .setGroup(GROUP_KEY_NEW_EPISODES)
                        .setGroupSummary(true)
                        .setTimeoutAfter(TIMEOUT)
                        .setStyle(inboxStyle);


        String pkg = argContext.getApplicationContext().getPackageName();

        Intent deleteIntent = new Intent(notificationDeleteAction).setPackage(pkg);
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(argContext.getApplicationContext(), REQUEST_CODE, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent openIntent = new Intent(argContext.getApplicationContext(), MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.putExtra(FragmentContainerActivity.STARTUP_FRAGMENT_KEY, FragmentContainerActivity.SUBSCRIPTION);
        PendingIntent pendingOpenIntent = PendingIntent.getActivity(argContext.getApplicationContext(), 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingOpenIntent);
        mBuilder.setDeleteIntent(pendingDeleteIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setCategory(Notification.CATEGORY_EMAIL);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) argContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(sProgressNotificationId, mBuilder.build());
    }

    public void removeNotification(@NonNull Context argContext) {
        mEpisodes.clear();
        mSubscriptions.clear();
        NotificationManager mNotificationManager =
                (NotificationManager) argContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.cancel(sProgressNotificationId);
    }

    private static boolean showEpisode(@NonNull IEpisode argEpisode, @NonNull Context argContext) {
        ISubscription iSubscription = argEpisode.getSubscription(argContext);

        if (iSubscription instanceof Subscription) {
            return ((Subscription) iSubscription).doNotifyOnNew(argContext);
        }

        return showNotificationAppSetting(argContext);
    }

    private static boolean showNotificationAppSetting(@NonNull Context argContext) {
        return PreferenceHelper.getBooleanPreferenceValue(argContext,
                R.string.pref_new_episode_notification_key,
                R.bool.pref_new_episode_notification_default);
    }

    private static Spanned getLine(@NonNull IEpisode argEpisode, @NonNull Context argContext) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String subscription = argEpisode.getSubscription(argContext).getTitle() + ":";
        String episode = " " + argEpisode.getTitle();
        return StrUtils.getTextWithBoldPrefix(builder, subscription, episode);
    }

}
