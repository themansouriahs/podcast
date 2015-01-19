package org.bottiger.podcast.provider;

import android.content.ContentResolver;

public abstract class AbstractItem implements WithIcon {

	public WithIcon createFromJSON(ContentResolver contentResolver, String json) {
		WithIcon item = null;
		if (this instanceof FeedItem) {
			item = FeedItem.fromJSON(contentResolver, json);
		} else if (this instanceof Subscription) {
			item = Subscription.fromJSON(contentResolver, json);
		}
		return item;
	}
	
	/**
	 * Update the Item in the database
	 */
	public void update(ContentResolver contentResolver) {
		update(contentResolver, false, false);
	}

}
