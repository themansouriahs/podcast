package org.bottiger.podcast.webservices.directories.audiosearch.types;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aplb on 12-12-2016.
 */

public class ShowImageFile {

    @SerializedName("full")
    String mFullImage;

    @SerializedName("thumb")
    String mThumbnail;

    public String getThumbnail() {
        return mThumbnail;
    }

}
