package org.bottiger.podcast.webservices.datastore.gpodder.datatypes;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Arvid on 8/25/2015.
 */
public class SubscriptionChanges implements Serializable {

    public long timestamp;
    public List<String> add = new LinkedList<>();
    public List<String> remove = new LinkedList<>();

}
