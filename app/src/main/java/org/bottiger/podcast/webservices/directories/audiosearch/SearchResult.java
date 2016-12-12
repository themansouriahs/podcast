package org.bottiger.podcast.webservices.directories.audiosearch;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.webservices.directories.audiosearch.types.SubscriptionResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 06-12-2016.
 */

class SearchResult {

    @SerializedName("results")
    private List<SubscriptionResult> mSubscriptions;

    @NonNull
    List<SlimSubscription> toSubscriptions() {

        LinkedList<SlimSubscription> subscriptions = new LinkedList<>();

        if (mSubscriptions == null) {
            return subscriptions;
        }

        for (int i = 0; i < mSubscriptions.size(); i++) {
            SlimSubscription subscription = mSubscriptions.get(i).toSubscription();
            if (subscription != null) {
                subscriptions.add(subscription);
            }
        }

        return subscriptions;

    }
}
