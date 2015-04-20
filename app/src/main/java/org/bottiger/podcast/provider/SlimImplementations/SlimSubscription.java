package org.bottiger.podcast.provider.SlimImplementations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by apl on 15-04-2015.
 */
public class SlimSubscription implements ISubscription, Parcelable {

    private String mTitle;
    private URL mURL;
    private String mImageURL;
    private ArrayList<SlimEpisode> mEpisodes = new ArrayList<>();

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

    public ArrayList<SlimEpisode> getEpisodes() {
        return mEpisodes;
    }

    public void getEpisodes(ArrayList<SlimEpisode> argEpisodes) {
        mEpisodes = argEpisodes;
    }

    @Override
    public TYPE getType() {
        return TYPE.SLIM;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        SlimEpisode[] episodeArray = mEpisodes.toArray(new SlimEpisode[mEpisodes.size()]);

        // I think the order is important
        out.writeString(mTitle);
        out.writeString(mImageURL);
        out.writeString(mURL.toString());
        out.writeParcelableArray(episodeArray, PARCELABLE_WRITE_RETURN_VALUE);
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
        mTitle = in.readString();
        mImageURL = in.readString();
        try {
            mURL = new URL(in.readString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        mEpisodes = in.readArrayList(SlimEpisode.class.getClassLoader());
    }
}
