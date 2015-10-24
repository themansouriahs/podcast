package org.bottiger.podcast.provider.SlimImplementations;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.model.EpisodeChanged;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by apl on 21-04-2015.
 */
public class SlimEpisode implements IEpisode, Parcelable {

    private String mTitle;
    private URL mUrl;
    private String mDescription;
    private long mDuration = -1;
    private int mPriority;
    private URL mArtworkUrl;
    private long mOffset;
    private long mFilesize = 0;

    public SlimEpisode() {
    }

    public SlimEpisode(@NonNull String argTitle,
                       @NonNull URL argUrl,
                       @NonNull String argDescription) {
        mTitle = argTitle;
        mUrl = argUrl;
        mDescription = argDescription;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public URL getUrl() {
        return mUrl;
    }

    @Nullable
    @Override
    public String getArtwork(@NonNull Context argContext) {
        String artwork = getArtwork();
        if (artwork == null)
            return null;

        return artwork;
    }

    @Override
    public String getArtwork() {
        return mArtworkUrl.toString();
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public String getAuthor() {
        return ""; // FIXME
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public int getPriority() {
        return mPriority;
    }

    @Override
    public ISubscription getSubscription(@NonNull Context argContext) {
        return null;
    }

    @Override
    public long getOffset() {
        return mOffset;
    }

    @Override
    public Date getDateTime() {
        return null;
    }

    @Override
    public Date getCreatedAt() {
        return new Date();
    }

    @Override
    public long getFilesize() { return mFilesize; }

    @Override
    public boolean isMarkedAsListened() { return false; }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public void setIsVideo(boolean argIsVideo) {

    }

    @Override
    public void setIsParsing(boolean argIsParsing) {
    }

    @Override
    public void setTitle(@NonNull String argTitle) {
        mTitle = argTitle;
    }

    @Override
    public void setUrl(@NonNull URL argUrl) {
        mUrl = argUrl;
    }

    @Override
    public void setArtwork(@NonNull URL argUrl) {
        mArtworkUrl = argUrl;
    }

    @Override
    public void setDescription(@NonNull String argDescription) {
        mDescription = argDescription;
    }

    @Override
    public void setDuration(long argDurationMs) {
        mDuration = argDurationMs;
    }

    @Override
    public void setPriority(@Nullable IEpisode argPrecedingItem, @NonNull Context argContext) {
        int precedingPriority = 0;
        if (argPrecedingItem != null) {
            precedingPriority = argPrecedingItem.getPriority();
        }

        mPriority = precedingPriority +1;
    }

    @Override
    public void removePriority() {
        mPriority = -1;
    }

    @Override
    public void setPriority(int argPriority) {
        mPriority = argPriority;
    }

    @Override
    public void setOffset(@Nullable ContentResolver contentResolver, long i) {
        mOffset = i;
    }

    @Nullable
    @Override
    public Uri getFileLocation(@Location int argLocation) {
        if (argLocation == REQUIRE_LOCAL || mUrl == null)
            return null;

        return Uri.parse(mUrl.toString());
    }

    @Override
    public boolean isDownloaded() {
        return false;
    }

    public int describeContents() {
        return 0;
    }

    public void setFilesize(long argFilesize) {
        mFilesize = argFilesize;
    }

    @Override
    public void setURL(String argUrl) {

    }

    @Override
    public void setAuthor(String argAuthor) {

    }

    @Override
    public void setPubDate(Date argPubDate) {

    }

    @Override
    public void setPageLink(String argPageLink) {

    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTitle);
        out.writeString(mUrl.toString());
        out.writeString(mDescription);
        out.writeLong(mFilesize);
        out.writeLong(mDuration);
    }

    public static final Parcelable.Creator<SlimEpisode> CREATOR
            = new Parcelable.Creator<SlimEpisode>() {
        public SlimEpisode createFromParcel(Parcel in) {
            return new SlimEpisode(in);
        }

        public SlimEpisode[] newArray(int size) {
            return new SlimEpisode[size];
        }
    };

    private SlimEpisode(Parcel in) {
        mTitle = in.readString();
        try {
            mUrl = new URL(in.readString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        mDescription = in.readString();
        mFilesize = in.readLong();
        mDuration = in.readLong();
    }
}
