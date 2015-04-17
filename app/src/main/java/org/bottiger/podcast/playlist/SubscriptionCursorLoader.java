package org.bottiger.podcast.playlist;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.adapters.FeedViewAdapter;
import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;

import android.app.Activity;
import android.database.Cursor;
import android.support.v4.app.Fragment;

public class SubscriptionCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 3;
	private Subscription subscription;

    public SubscriptionCursorLoader(Fragment fragment, FeedViewAdapter adapter, Cursor cursor) {
        super(fragment, adapter, cursor);
    }

    public SubscriptionCursorLoader(Activity activity, FeedViewAdapter adapter, Cursor cursor) {
        super(activity, adapter, cursor);
    }

    /*
	public SubscriptionCursorLoader(Fragment fragment, AbstractPodcastAdapter adapter, Subscription subscription) {
		super(fragment, (PlaylistAdapter)adapter, null);
		this.subscription = subscription;
		loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());
	}*/
	
	private String getOrder() {
		return ItemColumns.DATE + " DESC";
	}
	
	public String getWhere() {		
		String where = ItemColumns.STATUS + "==" + Subscription.STATUS_SUBSCRIBED;
		return where;
	}

}
