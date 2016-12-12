package org.bottiger.podcast.webservices.directories.audiosearch.types;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aplb on 12-12-2016.
 */
public class ChartItem {

    @SerializedName("id")
    long mAudioSearchId;

    public long getAudioSearchId() {
        return mAudioSearchId;
    }

}
