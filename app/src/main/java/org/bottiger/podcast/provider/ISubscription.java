package org.bottiger.podcast.provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by apl on 15-04-2015.
 */
public interface ISubscription {

    enum TYPE { DEFAULT, SLIM };

    @NonNull
    String getTitle();

    @NonNull
    URL getURL();

    @NonNull
    String getURLString();

    @Nullable
    String getImageURL();

    @NonNull
    ArrayList<? extends IEpisode> getEpisodes();

    void setImageURL(@Nullable String argUrl);
    void setTitle(@Nullable String argTitle);
    void setDescription(@Nullable String argDescription);
    void setURL(@Nullable String argUrl);

    boolean IsDirty();

    TYPE getType();
}
