package org.bottiger.podcast.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.URL;
import java.util.Date;

/**
 * Created by apl on 21-04-2015.
 */
public interface IEpisode {

    public String getTitle();
    public URL getUrl();
    public String getArtwork(@NonNull Context argContext);
    public String getDescription();
    public String getAuthor();
    public long getDuration();
    public int getPriority();
    public ISubscription getSubscription(@NonNull Context argContext);
    public long getOffset();
    public Date getDateTime();

    public void setTitle(@NonNull String argTitle);
    public void setUrl(@NonNull URL argUrl);
    public void setArtwork(@NonNull URL argUrl);
    public void setDescription(@NonNull String argDescription);
    public void setDuration(long argDurationMs);
    public void setPriority(IEpisode argPrecedingItem, @NonNull Context argContext);
    public void setOffset(@Nullable ContentResolver contentResolver, long i);

    public boolean isDownloaded();

}
