package org.bottiger.podcast.provider;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by apl on 15-04-2015.
 */
public interface ISubscription {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DEFAULT, SLIM})
    @interface Type {}
    int DEFAULT = 0;
    int SLIM = 1;

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

    void setLink(@Nullable String argLink);
    void addEpisode(@Nullable IEpisode episode);

    boolean IsDirty();
    boolean IsSubscribed();

    boolean IsRefreshing();
    void setIsRefreshing(boolean argIsRefreshing);

    @Type int getType();
}
