package org.bottiger.podcast.provider.SlimImplementations;

import android.os.Parcel;
import android.os.Parcelable;

import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 21-04-2015.
 */
public class SlimEpisode implements IEpisode, Parcelable {

    private String mTitle;
    private String mUrl;
    private String mDescription;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTitle);
        out.writeString(mUrl);
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
        mUrl = in.readString();
        mDescription = in.readString();
    }
}
