package org.bottiger.podcast.notification

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.support.v4.app.NotificationManagerCompat

import org.bottiger.podcast.R
import org.bottiger.podcast.provider.ISubscription

/**
 * Created by aplb on 20-06-2017.
 */
object NotificationChannels {

    const val CHANNEL_ID_PLAYER         = "player_channel"
    const val CHANNEL_ID_SUBSCRIPTION   = "subscription_channel"

    fun createChannels(argContext: Context) {
        createChannels(argContext)
        createPlayerChannel(argContext)
    }

    @TargetApi(26)
    fun createPlayerChannel(argContext: Context) {
        // The user-visible name of the channel.
        val resources = argContext.resources;
        val name = resources.getString(R.string.channel_name_player)
        val importance = NotificationManagerCompat.IMPORTANCE_MAX
        val mChannel = NotificationChannel(CHANNEL_ID_PLAYER, name, importance)

        val notificationManager = argContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel);
    }

    @TargetApi(26)
    fun getSubscriptionUpdatedChannel(argContext: Context) {
        // The user-visible name of the channel.
        val resources = argContext.resources;
        val name = resources.getString(R.string.channel_name_subscriptions)
        val importance = NotificationManagerCompat.IMPORTANCE_MIN
        val mChannel = NotificationChannel(CHANNEL_ID_SUBSCRIPTION, name, importance)

        val notificationManager = argContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel);
    }

}
