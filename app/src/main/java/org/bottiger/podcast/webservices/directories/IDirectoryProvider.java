package org.bottiger.podcast.webservices.directories;

import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;

import org.bottiger.podcast.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import okhttp3.OkHttpClient;

/**
 * Created by apl on 13-04-2015.
 */
public interface IDirectoryProvider {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SEARCH_RESULTS, BY_AUTHOR, POPULAR, TRENDING})
    @interface ListMode {}
    @StringRes int SEARCH_RESULTS = R.string.discovery_search_results;
    @StringRes int BY_AUTHOR = R.string.discovery_recommendations;
    @StringRes int POPULAR = R.string.discovery_recommendations_popular;
    @StringRes int TRENDING = R.string.discovery_recommendations_trending;

    int TOPLIST_AMOUNT = 10;

    // Result callback
    interface Callback {
        void result(@NonNull ISearchResult argResult);
        void error(@NonNull Exception argException);
    }

    @NonNull
    String getName();

    void search(@NonNull ISearchParameters argParameters,
                @NonNull Callback argCallback);
    void abortSearch();

    boolean isEnabled();

    @StringRes int[] supportedListModes();

    @MainThread void toplist(@NonNull Callback argCallback);
    @MainThread void toplist(int amount, @Nullable String argTag, @NonNull Callback argCallback);

    @NonNull OkHttpClient getOkHttpClient();

}
