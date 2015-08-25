package org.bottiger.podcast.webservices.datastore.gpodder.datatypes;

import java.util.List;

/**
 * Created by Arvid on 8/25/2015.
 */
public class UpdatedUrls {
    private long timestamp;
    private List<UpdatedUrl> updated_urls;
}

class UpdatedUrl {
    private List<String> urls;
}
