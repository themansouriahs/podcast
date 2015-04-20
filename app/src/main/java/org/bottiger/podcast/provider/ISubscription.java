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

    public enum TYPE { DEFAULT, SLIM };

    @NonNull
    public String getTitle();

    @NonNull
    public URL getURL();

    @NonNull
    public String getURLString();

    @Nullable
    public String getImageURL();

    @NonNull
    public ArrayList<? extends IEpisode> getEpisodes();

    public void setImageURL(@Nullable String argUrl);
    public void setTitle(@Nullable String argTitle);
    public void setDescription(@Nullable String argDescription);
    public void setURL(@Nullable String argUrl);

    public boolean IsDirty();

    public TYPE getType();
}
