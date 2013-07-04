package org.bottiger.podcast.parser;

import java.util.ArrayList;
import java.util.HashMap;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;

import android.content.ContentResolver;

public class FeedUpdater {
	
	private ContentResolver contentResolver;
	
	public FeedUpdater(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
	}
	
	public void updateDatabase(Subscription subscription, ArrayList<FeedItem> items) {
		
		int size = items.size();
		
		String[] urls = new String[size];
		for (int i = 0; i < size; i++) {
			urls[i] = items.get(i).getURL();
		}
		
		FeedItem[] localItems = FeedItem.getByURL(contentResolver, urls, null);
		
		HashMap<String,FeedItem> itemDict = new HashMap<String,FeedItem>();
		for (FeedItem item : localItems) {
			if (item != null)
				itemDict.put(item.getURL(), item);
		}
		
		subscription.update(contentResolver);
		
		FeedItem localItem;
		for (FeedItem item : items) {
			localItem = itemDict.get(item.getURL());
			
			if (localItem == null) {
				if (item.image == null)
					item.image = subscription.getImageURL();
				item.update(contentResolver);
			}
		}
		
	}

}
