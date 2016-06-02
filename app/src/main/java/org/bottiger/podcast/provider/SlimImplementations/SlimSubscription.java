package org.bottiger.podcast.provider.SlimImplementations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.view.View;

import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.converter.EpisodeConverter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by apl on 15-04-2015.
 */
public class SlimSubscription implements ISubscription, Parcelable {

    private String mTitle;
    private URL mURL;
    private String mImageURL;
    private EpisodeList<SlimEpisode> mEpisodes;

    private boolean mIsSubscribed = false;
    private boolean mIsDirty = false;

    public SlimSubscription(@NonNull URL argURL) {
        mTitle = "";
        mURL = argURL;
        mImageURL = "";
        initEpisodes();
    }

    public SlimSubscription(@NonNull String argTitle, @NonNull URL argURL, @Nullable String argImageURL) {
        mTitle = argTitle;
        mURL = argURL;
        mImageURL = argImageURL;
        initEpisodes();
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
        return mImageURL;
    }

    @Override
    public void setImageURL(@Nullable String argUrl) {
        mImageURL = argUrl;
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
    public void setLink(@Nullable String argLink) {

    }

    @Override
    public void addEpisode(@Nullable IEpisode episode) {
        SlimEpisode slimEpisode;
        boolean isSlimEpisode = episode instanceof SlimEpisode;
        if (!isSlimEpisode) {
            slimEpisode = EpisodeConverter.toSlim(episode);
        } else {
            slimEpisode = (SlimEpisode)episode;
        }
        mEpisodes.add(slimEpisode);
    }

    @Override
    public boolean IsDirty() {
        return mIsDirty;
    }

    public void markForSubscription(boolean argDoSubscribe) {
        setIsSubscribed(argDoSubscribe);
        mIsDirty = argDoSubscribe;
    }

    /**
     * See comment in the interface
     */
    @Override
    public boolean IsSubscribed() {
        return mIsSubscribed;
    }

    public void setIsSubscribed(boolean argIsSubscribed) {
        mIsSubscribed = argIsSubscribed;
    }

    @Override
    public boolean IsRefreshing() {
        return false;
    }

    @Override
    public void setIsRefreshing(boolean argIsRefreshing) {

    }

    @Override
    public boolean isListOldestFirst() {
        return false;
    }

    public EpisodeList getEpisodes() {
        return mEpisodes;
    }

    public void setEpisodes(EpisodeList argEpisodes) {
        mEpisodes = argEpisodes;
    }

    @Override
    public @Type int getType() {
        return ISubscription.SLIM;
    }

    @Override
    public int getPrimaryColor() {
        return -1;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        ArrayList<SlimEpisode> episodeArray = new ArrayList<>();

        for (int i = 0; i < mEpisodes.size(); i++) {
            episodeArray.add(mEpisodes.get(i));
        }

        // I think the order is important
        out.writeString(mTitle);
        out.writeString(mImageURL);
        out.writeString(mURL.toString());
    }

    public static final Parcelable.Creator<SlimSubscription> CREATOR
            = new Parcelable.Creator<SlimSubscription>() {
        public SlimSubscription createFromParcel(Parcel in) {
            return new SlimSubscription(in);
        }

        public SlimSubscription[] newArray(int size) {
            return new SlimSubscription[size];
        }
    };

    private SlimSubscription(Parcel in) {
        initEpisodes();
        mTitle = in.readString();
        mImageURL = in.readString();
        try {
            mURL = new URL(in.readString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void initEpisodes() {
        mEpisodes = new EpisodeList(SlimEpisode.class, new SortedList.Callback<SlimEpisode>() {
            @Override
            public int compare(SlimEpisode o1, SlimEpisode o2) {
                return 0;
            }

            @Override
            public void onInserted(int position, int count) {

            }

            @Override
            public void onRemoved(int position, int count) {

            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onChanged(int position, int count) {

            }

            @Override
            public boolean areContentsTheSame(SlimEpisode oldItem, SlimEpisode newItem) {
                return false;
            }

            @Override
            public boolean areItemsTheSame(SlimEpisode item1, SlimEpisode item2) {
                return false;
            }
        });
    }
}
