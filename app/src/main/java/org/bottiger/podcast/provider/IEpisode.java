package org.bottiger.podcast.provider;

import android.support.annotation.NonNull;

import java.net.URL;

/**
 * Created by apl on 21-04-2015.
 */
public interface IEpisode {

    public String getTitle();
    public URL getUrl();
    public String getDescription();

    public void setTitle(@NonNull String argTitle);
    public void setUrl(@NonNull URL argUrl);
    public void setDescription(@NonNull String argDescription);

}
