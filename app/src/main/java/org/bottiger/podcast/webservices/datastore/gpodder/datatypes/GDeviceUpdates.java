package org.bottiger.podcast.webservices.datastore.gpodder.datatypes;

import java.util.List;

/**
 * Created by Arvid on 8/26/2015.
 */
public class GDeviceUpdates {
    private List<GSubscription> add;
    private List<String> remove;
    private List<GEpisode> updates;
    private String timestamp;
}
