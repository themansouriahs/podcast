package org.bottiger.podcast.fragments.subscription

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v7.util.SortedList
import org.bottiger.podcast.model.datastructures.EpisodeList
import org.bottiger.podcast.provider.IEpisode
import org.bottiger.podcast.provider.ISubscription
import org.bottiger.podcast.provider.Subscription

/**
 * Created by aplb on 22-06-2017.
 */
class SubscriptionsViewModel(application : Application) : AndroidViewModel(application) {

    var liveSubscription: LiveData<SortedList<Subscription>> = MutableLiveData()
}