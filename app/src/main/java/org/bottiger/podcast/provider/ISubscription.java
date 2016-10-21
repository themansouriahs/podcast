package org.bottiger.podcast.provider;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.model.datastructures.EpisodeList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;

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

    void fetchImage(@NonNull Context argContext);

    @NonNull
    EpisodeList<IEpisode> getEpisodes();

    void setImageURL(@Nullable String argUrl);
    void setTitle(@Nullable String argTitle);
    void setDescription(@Nullable String argDescription);
    void setURL(@Nullable String argUrl);

    void setLink(@Nullable String argLink);
    boolean addEpisode(@Nullable IEpisode episode);

    boolean contains(@NonNull IEpisode argEpisode);

    boolean IsDirty();

    boolean doSkipIntro();

    @NonNull
    Integer getNewEpisodes();

    long getLastUpdate();

    /**
     * Determines if we are subscribed to a Subscription with the same URL
     * @return
     */
    boolean IsSubscribed();

    boolean IsRefreshing();
    void setIsRefreshing(boolean argIsRefreshing);

    boolean IsLoaded();

    boolean isListOldestFirst();

    @Type int getType();
    @ColorInt int getPrimaryColor();
}
