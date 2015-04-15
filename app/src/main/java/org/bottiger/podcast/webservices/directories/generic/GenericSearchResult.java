package org.bottiger.podcast.webservices.directories.generic;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.webservices.directories.ISearchResult;

import java.util.LinkedList;

/**
 * Created by apl on 13-04-2015.
 */
public class GenericSearchResult implements ISearchResult {

    private LinkedList<ISubscription> mSubscriptions = new LinkedList<>();

    public void addResult(@NonNull ISubscription argSubscription) {
        mSubscriptions.add(argSubscription);
    }

    @NonNull
    @Override
    public Iterable<ISubscription> getResults() {
        return mSubscriptions;
    }
}
