package org.bottiger.podcast.webservices.directories.generic;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.bottiger.podcast.DiscoveryFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchResult;

/**
 * Created by apl on 13-04-2015.
 */
public abstract class GenericDirectory implements IDirectoryProvider{

    private String mName;

    public GenericDirectory(@NonNull String argName) {
        mName = argName;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public static @StringRes int getNameRes() {
        return R.string.webservices_discovery_engine_unknown;
    }

    public void abortSearch() {
        AsyncTask task = getAsyncTask();
        if (task == null)
            return;

        task.cancel(true);
    }

    @Override
    public void toplist(@NonNull Callback argCallback) {
        toplist(TOPLIST_AMOUNT, null, argCallback);
    }

    public boolean isEnabled() {
        return true;
    }

    protected abstract AsyncTask<String, Void, ISearchResult> getAsyncTask();

}
