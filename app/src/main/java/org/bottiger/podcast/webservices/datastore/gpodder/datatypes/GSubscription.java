package org.bottiger.podcast.webservices.datastore.gpodder.datatypes;

import android.support.annotation.NonNull;

/**
 * Created by Arvid on 8/25/2015.
 */
public class GSubscription {
    private int subscribers;
    private int subscribers_last_week;
    private int position_last_week;
    private String website;
    private String url;
    private String logo_url;
    private String title;
    private String description;
    private String mygpo_link;
    private String scaled_logo_url;

    @NonNull
    public String getTitle() {
        return title != null ? title : "";
    }

    @NonNull
    public String getUrl() {
        return url != null ? url : "";
    }

    @NonNull
    public String getLogoUrl() {
        return logo_url != null ? logo_url : "";
    }

    @NonNull
    public void setUrl(String argUrl) {
        this.url = argUrl;
    }
}
