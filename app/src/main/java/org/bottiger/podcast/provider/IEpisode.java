package org.bottiger.podcast.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Date;

/**
 * Created by apl on 21-04-2015.
 */
public interface IEpisode {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREFER_LOCAL, REQUIRE_LOCAL, REQUIRE_REMOTE})
    @interface Location {}
    int PREFER_LOCAL = 1;
    int REQUIRE_LOCAL = 2;
    int REQUIRE_REMOTE = 3;

    String getTitle();
    URL getUrl();
    @Deprecated @Nullable String getArtwork(@NonNull Context argContext);
    @Nullable String getArtwork();
    String getDescription();
    String getAuthor();
    long getDuration();
    int getPriority();
    ISubscription getSubscription(@NonNull Context argContext);
    long getOffset();
    Date getDateTime();
    Date getCreatedAt();
    long getFilesize();

    void setTitle(@NonNull String argTitle);
    void setUrl(@NonNull URL argUrl);
    void setArtwork(@NonNull URL argUrl);
    void setDescription(@NonNull String argDescription);
    void setDuration(long argDurationMs);
    void setOffset(@Nullable ContentResolver contentResolver, long i);

    @Nullable
    Uri getFileLocation(@Location int argLocation);

    /**
     * -1 => nothing
     * 0 => current playing item
     * 1 => first item in the playlist
     * 2 => second item in the playist
     *
     * @param argPriority
     */
    void setPriority(int argPriority);
    void setPriority(IEpisode argPrecedingItem, @NonNull Context argContext);
    void removePriority();

    boolean isDownloaded();
    boolean isMarkedAsListened();

    boolean isVideo();
    void setIsVideo(boolean argIsVideo);

}
