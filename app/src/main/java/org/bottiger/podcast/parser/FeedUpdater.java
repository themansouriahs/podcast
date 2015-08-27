package org.bottiger.podcast.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PodcastProvider;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
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
		
		HashMap<String,FeedItem> itemDict = new HashMap<>();
		for (FeedItem item : localItems) {
			if (item != null)
				itemDict.put(item.getURL(), item);
		}
		
		subscription.update(contentResolver);

        Playlist playlist = null;
        LinkedList<ContentValues> cvs = new LinkedList<>();
        HashMap<String, Integer> duplicateTest = new HashMap<>();

		FeedItem localItem;
        int counter = 0;
		for (FeedItem item : items) {
			localItem = itemDict.get(item.getURL());
			
			if (localItem == null) {
				if (item.image == null) {
                    item.image = subscription.getImageURL();
                }

				//item.update(contentResolver);
                if (item.url != null && !duplicateTest.containsKey(item.url)) {
                    cvs.add(item.getContentValues(false));
                    duplicateTest.put(item.url, counter);
                }
			}
		}

        ContentValues[] contentValuesArray = cvs.toArray(new ContentValues[cvs.size()]);

        try {
            int rowsInserted = contentResolver.bulkInsert(ItemColumns.URI, contentValuesArray);
			if (rowsInserted > 0) {
				subscription.setLastItemUpdated(System.currentTimeMillis());
				subscription.update(contentResolver);
			}

            if (playlist != null) {
                playlist.notifyDatabaseChanged();
            }
        } catch (SQLiteConstraintException e) {
            FeedItem[] localItems2 = FeedItem.getByURL(contentResolver, urls, null);
            return;
        }
		
	}

}
