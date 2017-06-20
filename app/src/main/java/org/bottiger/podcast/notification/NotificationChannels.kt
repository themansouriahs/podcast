package org.bottiger.podcast.notification

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
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

    @TargetApi(26)
    fun createPlayerChannel(argResources: Resources, argNotificationManager: NotificationManager) {
        // The user-visible name of the channel.
        val name = argResources.getString(R.string.channel_name_player)
        // The user-visible description of the channel.
        val description = argResources.getString(R.string.channel_description_player)
        val importance = NotificationManagerCompat.IMPORTANCE_MAX
        val mChannel = NotificationChannel(CHANNEL_ID_PLAYER, name, importance)

        argNotificationManager.createNotificationChannel(mChannel);
    }

    @TargetApi(26)
    fun getSubscriptionUpdatedChannel(argResources: Resources, argSubscription: ISubscription): NotificationChannel {
        // The user-visible name of the channel.
        val name = argSubscription.title; //argResources.getString(R.string.channel_name_player)
        // The user-visible description of the channel.
        val description = argResources.getString(R.string.channel_description_player)
        val importance = NotificationManagerCompat.IMPORTANCE_MIN
        val mChannel = NotificationChannel(CHANNEL_ID_SUBSCRIPTION, name, importance)

        return mChannel
    }

}
