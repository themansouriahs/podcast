package org.bottiger.podcast.playlist;

import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;

import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;

public class SubscriptionCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 3;
	private Subscription subscription;

	public SubscriptionCursorLoader(Fragment fragment, CursorAdapter adapter, Subscription subscription) {
		super(fragment, adapter);
		this.subscription = subscription;
		loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());
	}

	@Override
	public void cursorPostProsessing(ReorderCursor cursoer) {
	}
	
	private String getOrder() {
		return ItemColumns.DATE + " DESC";
	}
	
	public String getWhere() {		
		String where = ItemColumns.SUBS_ID + "=" + subscription.getId();
		return where;
	}

}
