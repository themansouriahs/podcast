package org.bottiger.podcast.playlist;

import org.bottiger.podcast.adapters.SubscriptionCursorAdapter;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;

import android.database.Cursor;
import android.support.v4.app.Fragment;

public class SubscriptionCursorLoader extends GenericSubscriptionLoader {
	
	private final int LOADER_ID = 3;
	private Subscription subscription;

    public SubscriptionCursorLoader(Fragment fragment, SubscriptionCursorAdapter adapter, Cursor cursor) {
        super(fragment, adapter, cursor);
        loadCursor(LOADER_ID, SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, getWhere(), getOrder());
    }

    String getWhere() {
        String whereClause = SubscriptionColumns.STATUS + "<>"
                + Subscription.STATUS_UNSUBSCRIBED;
        whereClause = whereClause + " OR " + SubscriptionColumns.STATUS
                + " IS NULL"; // Deprecated.
        return whereClause;
    }

    String getOrder() {
        return SubscriptionColumns.TITLE + " ASC";
    }

}
