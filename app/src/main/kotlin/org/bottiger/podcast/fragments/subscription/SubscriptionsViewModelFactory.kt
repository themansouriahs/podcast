package org.bottiger.podcast.fragments.subscription

import android.arch.lifecycle.ViewModel
import android.app.Application
import android.arch.lifecycle.ViewModelProvider
import org.bottiger.podcast.model.Library

/**
 * Created by aplb on 23-06-2017.
 */
class SubscriptionsViewModelFactory(application: Application, library: Library) : ViewModelProvider.NewInstanceFactory() {

    val application: Application
    val library: Library

    init {
        this.application = application
        this.library = library
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SubscriptionsViewModel(application, library) as T
    }
}