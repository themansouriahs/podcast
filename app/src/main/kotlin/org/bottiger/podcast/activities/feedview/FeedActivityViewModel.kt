package org.bottiger.podcast.activities.feedview

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.bottiger.podcast.provider.IEpisode
import org.bottiger.podcast.provider.ISubscription

/**
 * Created by aplb on 21-06-2017.
 */
class FeedActivityViewModel(application : Application) : AndroidViewModel(application) {

    val liveEpisodes:       MutableLiveData<List<IEpisode>> = MutableLiveData()
    var liveSubscription: LiveData<ISubscription> = MutableLiveData()

    fun getSubscriptions() {

    }
}