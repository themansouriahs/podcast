package org.bottiger.podcast.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.utils.chapter.Chapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Created by apl on 21-04-2015.
 */
public interface IEpisode extends IDbItem {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREFER_LOCAL, REQUIRE_LOCAL, REQUIRE_REMOTE})
    @interface Location {}
    int PREFER_LOCAL = 1;
    int REQUIRE_LOCAL = 2;
    int REQUIRE_REMOTE = 3;

    String getTitle();
    URL getUrl();
    @Nullable String getArtwork(@NonNull Context argContext);
    @NonNull String getDescription();
    String getAuthor();
    long getDuration();
    int getPriority();
    @NonNull ISubscription getSubscription(@NonNull Context argContext);
    long getOffset();
    Date getDateTime();
    Date getCreatedAt();
    long getFilesize();

    void setChapters(@NonNull List<Chapter> argChapters);
    @NonNull List<Chapter> getChapters();
    boolean hasChapters();

    void setTitle(@NonNull String argTitle);
    void setUrl(@NonNull URL argUrl);
    void setArtwork(@NonNull String argUrl);
    void setDescription(@NonNull String argDescription);
    boolean setDuration(long argDurationMs);

    boolean setOffset(long i);
    boolean seekTo(long msec);

    @Deprecated
    void setOffset(@Nullable ContentResolver contentResolver, long i);

    float getPlaybackSpeed(@NonNull Context argContext);

    double getProgress();
    void setProgress(double argProgress);

    boolean newerThan(IEpisode item);

    @NonNull
    String getURL();

    @Nullable
    Uri getFileLocation(@Location int argLocation, @NonNull Context argContext);

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

    boolean canDownload();
    boolean isDownloaded(@NonNull Context argContext);
    boolean isMarkedAsListened();
    boolean isNew();
    boolean isPlaying();
    boolean hasBeenDownloadedOnce();

    boolean isVideo();
    void setIsVideo(boolean argIsVideo);

    void setIsParsing(boolean argIsParsing);
    void setIsParsing(boolean argIsParsing, boolean doNotify);
    void setFilesize(long argFilesize);
    void setURL(String argUrl);
    void setAuthor(String argAuthor);
    void setPubDate(Date argPubDate);
    void setPageLink(String argPageLink);

}
