package org.bottiger.podcast.webservices.datastore.gpodder.api;

import android.support.annotation.NonNull;

import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;

/**
 * Created by Arvid on 8/25/2015.
 */
public class APIBase {

    @NonNull GPodderAPI mService;

    public APIBase(@NonNull GPodderAPI argGPodderAPI) {
        mService = argGPodderAPI;
    }

    @NonNull
    protected GPodderAPI getService() {
        return mService;
    }
}
