package org.bottiger.podcast.webservices.directories.generic;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.bottiger.podcast.DiscoveryFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.HttpUtils;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchResult;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by apl on 13-04-2015.
 */
public abstract class GenericDirectory implements IDirectoryProvider {

    private OkHttpClient mOkHttpClient;

    private Context mContext;
    private String mName;

    public GenericDirectory(@NonNull String argName, @NonNull Context argContext) {
        mName = argName;
        mContext = argContext;
        mOkHttpClient = createOkHttpClient();
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @StringRes public static int defaultMode() {
        return BY_AUTHOR;
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

    protected OkHttpClient createOkHttpClient() {
        return HttpUtils.getNewDefaultOkHttpClientBuilder(mContext).build();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

}
