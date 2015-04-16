package org.bottiger.podcast.webservices.directories.generic;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.bottiger.podcast.webservices.directories.IDirectoryProvider;

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

    public void abortSearch() {
        AsyncTask task = getAsyncTask();
        if (task == null)
            return;

        task.cancel(true);
    }

    protected abstract AsyncTask getAsyncTask();

}
