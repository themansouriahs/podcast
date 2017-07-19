package org.bottiger.podcast.provider;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.base.BaseSubscription;

import java.net.URL;

/**
 * Created by aplb on 29-11-2016.
 */

public class AudioBook extends BaseSubscription {

    @NonNull URL mURL;
    @NonNull String mTitle;

    public AudioBook(@NonNull URL argUrl) {
        mURL = argUrl;
    }

    @NonNull
    @Override
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    @Override
    public URL getURL() {
        return mURL;
    }

    @NonNull
    @Override
    public String getURLString() {
        return mURL.toString();
    }

    @Nullable
    @Override
    public String getImageURL() {
        return null;
    }

    @Override
    public void fetchImage(@NonNull Context argContext) {
    }

    @Override
    public boolean addEpisode(@Nullable IEpisode episode) {
        return false;
    }

    @Override
    public boolean IsDirty() {
        return false;
    }

    @Override
    public boolean doShowListened() {
        return true;
    }

    @NonNull
    @Override
    public Integer getNewEpisodes() {
        return null;
    }

    @Override
    public long getLastUpdate() {
        return 0;
    }

    @Override
    public boolean IsSubscribed() {
        return false;
    }

    @Override
    public boolean IsRefreshing() {
        return false;
    }

    @Override
    public void setIsRefreshing(boolean argIsRefreshing) {

    }

    @Override
    public boolean IsLoaded() {
        return false;
    }

    @Override
    public boolean isListOldestFirst(@NonNull Resources argResources) {
        return false;
    }

    @Override
    public void setListOldestFirst(boolean listOldestFirst) {

    }

    @Override
    public int getType() {
        return AUDIOBOOK;
    }

    @Override
    public int getPrimaryColor() {
        return 0;
    }

    @Override
    public int getPrimaryTintColor() {
        return 0;
    }

    @Override
    public int getSecondaryColor() {
        return 0;
    }

    @Override
    public String getPaletteUrl() {
        return null;
    }
}
