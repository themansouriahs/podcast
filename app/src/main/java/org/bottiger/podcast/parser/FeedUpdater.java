package org.bottiger.podcast.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PodcastProvider;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.views.PlayerButtonView;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;

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

        Playlist playlist = null;
        LinkedList<ContentValues> cvs = new LinkedList<ContentValues>();
        HashMap<String, Integer> duplicateTest = new HashMap<String, Integer>();

		FeedItem localItem;
        int counter = 0;
		for (FeedItem item : items) {
			localItem = itemDict.get(item.getURL());
			
			if (localItem == null) {
				if (item.image == null) {
                    item.image = subscription.getImageURL();
                }

                try {
                    playlist = Playlist.getActivePlaylist();
                    //playlist.notifyAbout(item);
                } catch (IllegalStateException ise) {
                    // its okay. We are running in a background job.
                }

				//item.update(contentResolver);
                if (item.url != null && !duplicateTest.containsKey(item.url)) {
                    cvs.add(item.getContentValues(false));
                    duplicateTest.put(item.url,counter);
                }
			}
		}

        ContentValues[] contentValuesArray = cvs.toArray(new ContentValues[cvs.size()]);

        try {
            contentResolver.bulkInsert(ItemColumns.URI, contentValuesArray);
            if (playlist != null) {
                playlist.notifyPlaylistChanged();
            }
        } catch (SQLiteConstraintException e) {
            FeedItem[] localItems2 = FeedItem.getByURL(contentResolver, urls, null);
            return;
        }
		
	}

}
