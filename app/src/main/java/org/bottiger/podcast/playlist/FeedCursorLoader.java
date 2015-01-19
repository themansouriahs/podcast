package org.bottiger.podcast.playlist;

import android.support.v4.app.Fragment;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.adapters.FeedViewAdapter;
import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;

/**
 * Created by apl on 02-09-2014.
 */
public class FeedCursorLoader extends GenericCursorLoader {

    private final int LOADER_ID = 4;
    private Subscription subscription;
    private FeedViewAdapter mAdapter = null;

    public FeedCursorLoader(Fragment fragment, FeedViewAdapter adapter, Cursor argCursor, Subscription subscription) {
        super(fragment, adapter, argCursor);
        this.subscription = subscription;
        this.mAdapter = adapter;
        //requery();
    }

    public void setSubscription(Subscription argSubscription) {
        subscription = argSubscription;
    }

    public void requery() {
        loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());
    }

    private String getOrder() {
        return ItemColumns.DATE + " DESC";
    }

    public String getWhere() {
        long id = subscription == null ? 5 : subscription.getId();
        String where = ItemColumns.SUBS_ID + "=" + id;
        return where;
    }
}
