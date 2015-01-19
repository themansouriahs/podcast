package org.bottiger.podcast.playlist;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;

import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;

public class SubscriptionCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 3;
	private Subscription subscription;

	public SubscriptionCursorLoader(Fragment fragment, AbstractPodcastAdapter adapter, Subscription subscription) {
		super(fragment, (ItemCursorAdapter)adapter, null);
		this.subscription = subscription;
		loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());
	}
	
	private String getOrder() {
		return ItemColumns.DATE + " DESC";
	}
	
	public String getWhere() {		
		String where = ItemColumns.SUBS_ID + "=" + subscription.getId();
		return where;
	}

}
