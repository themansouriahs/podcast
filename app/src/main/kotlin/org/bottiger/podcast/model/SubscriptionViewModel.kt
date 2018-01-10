package org.bottiger.podcast.model

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.support.v7.util.SortedList
import org.bottiger.podcast.SoundWaves
import org.bottiger.podcast.provider.IEpisode
import org.bottiger.podcast.provider.ISubscription
import org.bottiger.podcast.provider.Subscription

/**
 * Created by aplb on 21-06-2017.
 */
class SubscriptionViewModel: AndroidViewModel {

    val subscriptions : LiveData<List<Subscription>>

    constructor(application: SoundWaves) : super(application) {
         subscriptions = application.libraryInstance.liveSubscriptions;
    }

    /*
    constructor(argSharedPreferences: SharedPreferences) : super(argSharedPreferences) {}
    constructor(argSharedPreferences: SharedPreferences, url: String) : super(argSharedPreferences) {}
    constructor(argSharedPreferences: SharedPreferences, argSlimSubscription: ISubscription) : super(argSharedPreferences, argSlimSubscription) {}

        override fun getLiveEpisodes(): MutableLiveData<MutableList<IEpisode>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    */

}
