package org.bottiger.podcast.webservices.directories.stitcher;

import android.support.annotation.NonNull;

import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.generic.GenericDirectory;

/**
 * Created by apl on 13-04-2015.
 */
public class Stitcher extends GenericDirectory {

    private static final String NAME = "Stitcher";

    public Stitcher() {
        super(NAME);
    }

    @Override
    public void search(@NonNull ISearchParameters argParameters, @NonNull Callback argCallback) {

    }
}
