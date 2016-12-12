package org.bottiger.podcast.webservices.directories.audiosearch.types;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by aplb on 12-12-2016.
 */
public class Chart {

    @SerializedName("country")
    String mCountry;

    @SerializedName("limit")
    long mLimit;

    @SerializedName("start_date")
    String mDateString;

    @SerializedName("shows")
    Map<String, ChartItem> mShows;

    public Map<String, ChartItem> getShows() {
        return mShows;
    }

}
