package org.bottiger.podcast.provider;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.utils.ColorExtractor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;

import io.reactivex.Single;

/**
 * Created by apl on 15-04-2015.
 */
public interface ISubscription extends PaletteListener, IDbItem {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DEFAULT, SLIM, AUDIOBOOK})
    @interface Type {}
    int DEFAULT = 0;
    int SLIM = 1;
    int AUDIOBOOK = 2;

    @NonNull
    String getTitle();

    @NonNull
    URL getURL();

    @NonNull
    String getURLString();

    @Nullable
    String getImageURL();

    @Nullable
    String getDescription();

    boolean isPinned();

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

    boolean isListOldestFirst(@NonNull Resources argResources);
    void setListOldestFirst(boolean listOldestFirst);

    @Type int getType();
    @ColorInt int getPrimaryColor();
    @ColorInt int getPrimaryTintColor();
    @ColorInt int getSecondaryColor();

    void setPrimaryColor(@ColorInt int argColor);
    void setPrimaryTintColor(@ColorInt int argColor);
    void setSecondaryColor(@ColorInt int argColor);

    Single<ColorExtractor> getColors(@NonNull final Context argContext);

    void cacheImage(@NonNull final Context argContext);
}
