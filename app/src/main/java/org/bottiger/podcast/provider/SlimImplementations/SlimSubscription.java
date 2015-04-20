package org.bottiger.podcast.provider.SlimImplementations;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.ISubscription;

import java.net.URL;

/**
 * Created by apl on 15-04-2015.
 */
public class SlimSubscription implements ISubscription {

    private String mTitle;
    private URL mURL;
    private String mImageURL;

    public SlimSubscription(@NonNull String argTitle, @NonNull URL argURL, @Nullable String argImageURL) {
        mTitle = argTitle;
        mURL = argURL;
        mImageURL = argImageURL;
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
        return "";
    }

    @Nullable
    @Override
    public String getImageURL() {
        return mImageURL;
    }

    @Override
    public void setImageURL(@Nullable String argUrl) {

    }

    @Override
    public void setTitle(@Nullable String argTitle) {

    }

    @Override
    public void setDescription(@Nullable String argDescription) {

    }

    @Override
    public void setURL(@Nullable String argUrl) {

    }

    @Override
    public boolean IsDirty() {
        return false;
    }

    @Override
    public TYPE getType() {
        return TYPE.SLIM;
    }
}
