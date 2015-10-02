package org.bottiger.podcast.webservices.directories;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by apl on 13-04-2015.
 */
public interface IDirectoryProvider {

    int TOPLIST_AMOUNT = 20;

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

    void toplist(@NonNull Callback argCallback);
    void toplist(int amount, @Nullable String argTag, @NonNull Callback argCallback);

}
