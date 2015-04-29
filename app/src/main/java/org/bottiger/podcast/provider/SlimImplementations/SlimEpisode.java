package org.bottiger.podcast.provider.SlimImplementations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.IEpisode;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by apl on 21-04-2015.
 */
public class SlimEpisode implements IEpisode, Parcelable {

    private String mTitle;
    private URL mUrl;
    private String mDescription;

    public SlimEpisode(@NonNull String argTitle, @NonNull URL argUrl, @NonNull String argDescription) {
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
        return null;
    }

    @Override
    public String getDescription() {
        return null;
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
    public void setDescription(@NonNull String argDescription) {
        mDescription = argDescription;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTitle);
        out.writeString(mUrl.toString());
        out.writeString(mDescription);
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
    }
}
