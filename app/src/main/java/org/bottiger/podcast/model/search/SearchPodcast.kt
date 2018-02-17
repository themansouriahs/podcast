package org.bottiger.podcast.model.search

import org.bottiger.podcast.model.Library
import org.bottiger.podcast.provider.IEpisode
import org.bottiger.podcast.provider.ISubscription
import org.bottiger.podcast.provider.Subscription

/**
 * Created by bottiger on 17/02/2018.
 */
class SearchPodcast(val library: Library) {

    fun findSubscription(searchTerm: String) : ISubscription? {

        val subscriptions = library.liveSubscriptions.value.orEmpty();

        subscriptions.forEach {
            if (searchTerm == it.title.toLowerCase()) {
                return it;
            }
        }

        return null
    }

    fun findEpisode(searchTerm: String) : IEpisode? {

        val episodes = library.episodes;

        episodes.forEach {
            if (searchTerm == it.title) {
                return it;
            }
        }

        return null
    }

}