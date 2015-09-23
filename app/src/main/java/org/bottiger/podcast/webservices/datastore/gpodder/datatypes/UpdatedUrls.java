package org.bottiger.podcast.webservices.datastore.gpodder.datatypes;

import android.support.annotation.NonNull;
import android.util.ArraySet;
import android.util.Pair;

import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Arvid on 8/25/2015.
 */
public class UpdatedUrls {
    private long timestamp;
    //private List<List<String>> updated_urls;
    private String[][] updated_urls;

    private Set<Pair<String,String>> updatedUrls = null;

    @NonNull
    public Set<Pair<String,String>> getUpdatedUrls() {
        if (updatedUrls != null)
            return updatedUrls;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            updatedUrls = new ArraySet<>();
        } else {
            updatedUrls = new HashSet<>();
        }

        for (int i = 0; i < updated_urls.length; i++) {
            String[] urls = updated_urls[0];
            String oldUrl = urls[0];
            String newUrl = urls[1];

            Pair<String,String> pair = new Pair<>(oldUrl, newUrl);
            updatedUrls.add(pair);

        }

        return updatedUrls;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
