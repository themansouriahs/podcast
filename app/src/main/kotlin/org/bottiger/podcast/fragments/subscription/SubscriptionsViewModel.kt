package org.bottiger.podcast.fragments.subscription

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.MutableLiveData
import android.support.v7.util.SortedList
import io.reactivex.BackpressureStrategy
import org.bottiger.podcast.SoundWaves
import org.bottiger.podcast.model.Library
import org.bottiger.podcast.model.events.SubscriptionChanged
import org.bottiger.podcast.provider.Subscription

/**
 * Created by aplb on 22-06-2017.
 */
class SubscriptionsViewModel(application : Application, library: Library) : AndroidViewModel(application) {

    val liveSubscription: LiveData<Subscription>
    val liveSubscriptionChanged : LiveData<SubscriptionChanged>


    init {
        val type = Subscription::class.java
        val publisher = library.mSubscriptionsChangePublisher.toFlowable(BackpressureStrategy.LATEST).ofType(type)
        liveSubscription = LiveDataReactiveStreams.fromPublisher(publisher)

        val changeType = SubscriptionChanged::class.java
        val changedPublisher = SoundWaves.getRxBus2()
                                        .toFlowable()
                                        .ofType(changeType)
                                        .filter{it.action == SubscriptionChanged.CHANGED}

        liveSubscriptionChanged = LiveDataReactiveStreams.fromPublisher(changedPublisher)
    }
}

/*
        mRxSubscriptionChanged = SoundWaves.getRxBus()
                .toObserverable()
                .onBackpressureBuffer(10000)
                .ofType(SubscriptionChanged.class)
                .filter(new Func1<SubscriptionChanged, Boolean>() {
                    @Override
                    public Boolean call(SubscriptionChanged subscriptionChanged) {
                        return subscriptionChanged.getAction() == SubscriptionChanged.CHANGED;
                    }
                })
                .subscribe(new Action1<SubscriptionChanged>() {
                    @Override
                    public void call(SubscriptionChanged itemChangedEvent) {
                        Log.v(TAG, "Refreshing Subscription: " + itemChangedEvent.getId());
                        // Update the subscription fragment when a image is updated in an subscription
                        SortedList<Subscription> subscriptions = mLibrary.getSubscriptions();
                        Subscription subscription = mLibrary.getSubscription(itemChangedEvent.getId());

                        int index = subscriptions.indexOf(subscription); // doesn't work
                        for (int i = 0; i < subscriptions.size(); i++) {
                            Subscription currentSubscription = subscriptions.get(i);
                            if (subscription != null && subscription.equals(currentSubscription)) {
                                index = i;
                                break;
                            }
                        }

                        if (!mGridView.isComputingLayout()) {
                            mAdapter.notifyItemChanged(index);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.handleException(throwable);
                        Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                    }
                });

 */