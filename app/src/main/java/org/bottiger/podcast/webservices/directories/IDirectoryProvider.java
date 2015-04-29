package org.bottiger.podcast.webservices.directories;

import android.support.annotation.NonNull;

/**
 * Created by apl on 13-04-2015.
 */
public interface IDirectoryProvider {

    // Result callback
    interface Callback {
        public void result(@NonNull ISearchResult argResult);
        public void error(@NonNull Exception argException);
    }

    @NonNull
    public String getName();

    public void search(@NonNull ISearchParameters argParameters,
                       @NonNull Callback argCallback);
    public void abortSearch();

}
