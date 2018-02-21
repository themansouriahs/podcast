package org.bottiger.podcast.service

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.text.TextUtils
import org.bottiger.podcast.R
import org.bottiger.podcast.model.Library
import org.bottiger.podcast.playlist.Playlist
import org.bottiger.podcast.provider.IDbItem
import org.bottiger.podcast.provider.IEpisode
import org.bottiger.podcast.provider.ISubscription
import org.bottiger.podcast.provider.Subscription
import org.bottiger.podcast.utils.ErrorUtils
import org.bottiger.podcast.utils.StrUtils
import org.bottiger.podcast.views.PlaylistScrollerParent
import javax.inject.Inject

/**
 * Created by arvid on 09/11/2017.
 */
class  MediaBrowserHelper(private var context: Context, private var library: Library, private var playlist: Playlist) {

    val TAG = MediaBrowserHelper::class.java.name

    companion object {
        const val BROWSER_ROOT_ID = "BROWSER_ROOT"
        const val BROWSER_PLAYLIST_ID = "BROWSER_PLAYLIST"
        const val BROWSER_SUBSCRIPTION_PREFIX = "BRUWSER_SUBSCRIPTION"
        const val BROWSER_EPISODE_PREFIX = "BRUWSER_EPISODE"
        const val BROWSER_DELIMITER = "\r"

        fun parseMediaId(mediaId: String, library: Library) : IDbItem? {
            val parts = mediaId.split(BROWSER_DELIMITER.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            if (parts.size < 2) {
                ErrorUtils.handleException(IllegalArgumentException(), "mediaID: " + mediaId)
                return null;
            }

            val type = parts[0]
            val url = parts[1]
            val id = if (TextUtils.isDigitsOnly(parts[1])) java.lang.Long.parseLong(parts[1]) else 0

            when (type) {
                BROWSER_SUBSCRIPTION_PREFIX -> return library.getSubscription(id)
                BROWSER_EPISODE_PREFIX      -> return library.getEpisode(url)
                else                        -> return null;
            }
        }

        fun calcMediaId(item : IDbItem) : String {
            when (item) {
                is IEpisode -> return BROWSER_EPISODE_PREFIX + BROWSER_DELIMITER + item.uuid;
                is Subscription -> return BROWSER_SUBSCRIPTION_PREFIX + BROWSER_DELIMITER + item.getId();
            }

            return "";
        }
    }

    val res = context.getResources()

    fun playlistItem() : MediaBrowserCompat.MediaItem {

        val playlistSize = playlist.size()

        val playlistTitle = context.getString(R.string.media_browser_playlist_title)
        val playlistDescription = context.getString(R.string.media_browser_playlist_description)
        val episodesCount = res.getQuantityString(R.plurals.media_browser_playlist_subtitle, playlistSize, playlistSize)

        val playlistIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_queue_music_black)

        val mediaDescriptionCompat = MediaDescriptionCompat.Builder()
        mediaDescriptionCompat.setTitle(playlistTitle)
        mediaDescriptionCompat.setDescription(playlistDescription)
        mediaDescriptionCompat.setSubtitle(episodesCount)
        mediaDescriptionCompat.setMediaId(BROWSER_PLAYLIST_ID)
        mediaDescriptionCompat.setIconBitmap(playlistIcon)
        val mediaDescription = mediaDescriptionCompat.build()

        val mediaItem = MediaBrowserCompat.MediaItem(mediaDescription, FLAG_BROWSABLE);

        return mediaItem;
    }

    fun SubscriptionItems() : List<MediaBrowserCompat.MediaItem> {
        val items : MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()

        // Add subscriptions
        val subscriptions = library.liveSubscriptions.value

        var mediaSubscriptionDescriptionCompat: MediaDescriptionCompat.Builder
        var subscriptionDescription: MediaDescriptionCompat
        var subscription: Subscription
        for (i in 0 until (subscriptions?.size ?: 0)) {
            subscription = subscriptions!!.get(i)

            val episodeCount = subscription.episodeCount
            val episodesCountStr = res.getQuantityString(R.plurals.media_browser_playlist_subtitle, episodeCount, episodeCount)

            //val mediaID = BROWSER_SUBSCRIPTION_PREFIX + BROWSER_DELIMITER + subscription.getId()
            val mediaID = calcMediaId(subscription)

            mediaSubscriptionDescriptionCompat = MediaDescriptionCompat.Builder()
            mediaSubscriptionDescriptionCompat.setTitle(subscription.title)
            mediaSubscriptionDescriptionCompat.setDescription(subscription.description)
            mediaSubscriptionDescriptionCompat.setSubtitle(episodesCountStr)
            mediaSubscriptionDescriptionCompat.setMediaId(mediaID)
            //mediaSubscriptionDescriptionCompat.setIconBitmap(playlistIcon);
            subscriptionDescription = mediaSubscriptionDescriptionCompat.build()

            val mediaItem = MediaBrowserCompat.MediaItem(subscriptionDescription, FLAG_BROWSABLE)
            items.add(mediaItem)
        }

        return items;
    }

    fun EpisodeItems(parentId : String): List<MediaBrowserCompat.MediaItem> {
        val items : MutableList<MediaBrowserCompat.MediaItem> = mutableListOf<MediaBrowserCompat.MediaItem>()

        val dbitem = parseMediaId(parentId, library) as? ISubscription ?: return items
        val subscription = dbitem;

        var mediaEpisodeDescriptionCompat: MediaDescriptionCompat.Builder
        var episodeDescription: MediaDescriptionCompat
        val episodes = subscription.getEpisodes().getUnfilteredList()
        var episode: IEpisode
        for (i in episodes.indices) {
            episode = episodes.get(i)

            //val mediaID = BROWSER_EPISODE_PREFIX + BROWSER_DELIMITER + episode.uuid
            val mediaID = calcMediaId(episode)
            val episodeTitle = StrUtils.formatTitle(episode.title)

            mediaEpisodeDescriptionCompat = MediaDescriptionCompat.Builder()
            mediaEpisodeDescriptionCompat.setTitle(episodeTitle)
            mediaEpisodeDescriptionCompat.setDescription(episode.description)
            mediaEpisodeDescriptionCompat.setSubtitle(subscription.getTitle())
            mediaEpisodeDescriptionCompat.setMediaId(mediaID)
            //mediaSubscriptionDescriptionCompat.setIconBitmap(playlistIcon);
            episodeDescription = mediaEpisodeDescriptionCompat.build()

            val mediaItem = MediaBrowserCompat.MediaItem(episodeDescription, FLAG_PLAYABLE)
            items.add(mediaItem)
        }

        return items
    }
}