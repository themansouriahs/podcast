package org.bottiger.podcast.provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URL;

/**
 * Created by apl on 15-04-2015.
 */
public interface ISubscription {

    @NonNull
    public String getTitle();

    @NonNull
    public URL getURL();

    @Nullable
    public String getImageURL();
}
