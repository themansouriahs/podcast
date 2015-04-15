package org.bottiger.podcast.webservices.directories.generic;

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

}
