package org.bottiger.podcast.notification

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.support.v4.app.NotificationManagerCompat

import org.bottiger.podcast.R
import org.bottiger.podcast.provider.ISubscription
import org.bottiger.podcast.provider.Subscription
import org.bottiger.podcast.provider.Subscription.STATUS_UNSUBSCRIBED

/**
 * Created by aplb on 20-06-2017.
 */
object NotificationChannels {

    const val CHANNEL_ID_PLAYER         = "player_channel"
    const val CHANNEL_ID_SUBSCRIPTION   = "subscription_channel"
    const val CHANNEL_ID_ALL_EPISODES   = "episodes_channel"

    @TargetApi(26)
    fun createPlayerChannel(argContext: Context) {
        // The user-visible name of the channel.
        val resources = argContext.resources;
        val name = resources.getString(R.string.channel_name_player)
        val importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID_PLAYER, name, importance)
        channel.setSound(null, null);

        val notificationManager = argContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //notificationManager.deleteNotificationChannel(CHANNEL_ID_PLAYER);

        notificationManager.createNotificationChannel(channel);
    }

    @TargetApi(26)
    fun createEpisodesChannel(argContext: Context) {
        // The user-visible name of the channel.
        val resources = argContext.resources;
        val name = resources.getString(R.string.channel_name_episodes)
        val importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID_ALL_EPISODES, name, importance)

        channel.setShowBadge(true)

        val notificationManager = argContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel);
    }

    @TargetApi(26)
    fun getSubscriptionUpdatedChannel(argContext: Context, argSubscription: Subscription) {
        // The user-visible name of the channel.
        val resources = argContext.resources;
        val name = argSubscription.title;
        val group = resources.getString(R.string.channel_name_subscriptions)
        val importance = NotificationManagerCompat.IMPORTANCE_MIN
        val channel_id = getChannelId(argSubscription)
        val channel = NotificationChannel(channel_id, name, importance)

        channel.group = group
        channel.setShowBadge(true)

        val notificationManager = argContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val remove = argSubscription.status.toInt() == STATUS_UNSUBSCRIBED

        if (remove) {
            notificationManager.deleteNotificationChannel(channel_id)
        } else {
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getChannelId(argSubscription: Subscription) : String {
        return CHANNEL_ID_SUBSCRIPTION + argSubscription.id
    }

}
