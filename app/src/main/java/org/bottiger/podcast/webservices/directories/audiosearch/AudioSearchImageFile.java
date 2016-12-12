package org.bottiger.podcast.webservices.directories.audiosearch;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aplb on 12-12-2016.
 */

public class AudioSearchImageFile {

    @SerializedName("id")
    long mAudioSearchId;

    @SerializedName("original_file_url")
    String mImageUrl;

    public String getImageUrl() {
        return mImageUrl;
    }
}
