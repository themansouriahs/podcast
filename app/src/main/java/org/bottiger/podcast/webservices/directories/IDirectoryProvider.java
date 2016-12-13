package org.bottiger.podcast.webservices.directories;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;

/**
 * Created by apl on 13-04-2015.
 */
public interface IDirectoryProvider {

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

    @MainThread void toplist(@NonNull Callback argCallback);
    @MainThread void toplist(int amount, @Nullable String argTag, @NonNull Callback argCallback);

}
