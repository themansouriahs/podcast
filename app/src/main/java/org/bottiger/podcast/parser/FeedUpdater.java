package org.bottiger.podcast.parser;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;
import android.support.v7.util.SortedList;

public class FeedUpdater {
	
	private ContentResolver contentResolver;
	
	public FeedUpdater(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
	}

	@Deprecated
	public void updateDatabase(Subscription subscription) {

		SortedList<IEpisode> items = subscription.getEpisodes();
		int size = items.size();
		
		ArrayList<String> urls = new ArrayList<>(size);
		URL url;
		for (int i = 0; i < size; i++) {
			url = items.get(i).getUrl();

			if(url != null) {
				urls.add(url.toString());
			}
		}

		String[] urlArray = urls.toArray(new String[urls.size()]);
		
		FeedItem[] localItems = null;//FeedItem.getByURL(contentResolver, urlArray, null);
		
		HashMap<String,FeedItem> itemDict = new HashMap<>();
		for (FeedItem item : localItems) {
			if (item != null && item.sub_id > 0)
				itemDict.put(item.getURL(), item);
		}
		
		//subscription.update(contentResolver);

		PlayerService ps = PlayerService.getInstance();
		Playlist playlist = null;

		if (ps != null) {
			playlist = PlayerService.getInstance().getPlaylist();
		}
        LinkedList<ContentValues> cvs = new LinkedList<>();
        HashMap<String, Integer> duplicateTest = new HashMap<>();

		FeedItem localItem;
        int counter = 0;
		//for (IEpisode item : items) {
		IEpisode item;
		for (int i = 0; i < items.size(); i++) {
			item = items.get(i);
			FeedItem feedItem = (FeedItem)item;
			localItem = itemDict.get(feedItem.getURL());
			
			if (localItem == null) {
				if (feedItem.image == null) {
					feedItem.image = subscription.getImageURL();
                }

				//item.update(contentResolver);
				ContentValues cv;
                if (feedItem.url != null && !duplicateTest.containsKey(feedItem.url)) {
					//cv = feedItem.getEpisodeContentValues(false);
					//cv = FeedItem.addCreatedAtToContentValues(cv);
                    //cvs.add(cv);
                    duplicateTest.put(feedItem.url, counter);
                }
			}
		}

        ContentValues[] contentValuesArray = cvs.toArray(new ContentValues[cvs.size()]);

        try {
            int rowsInserted = contentResolver.bulkInsert(ItemColumns.URI, contentValuesArray);
			if (rowsInserted > 0) {
				subscription.setLastItemUpdated(System.currentTimeMillis());
				//subscription.update(contentResolver);
			}

            if (playlist != null) {
                //playlist.notifyDatabaseChanged();
				playlist.refresh(SoundWaves.getAppContext());
            }
        } catch (SQLiteConstraintException e) {

            // There are already some corrupted episodes in the database.
            // We should get rid of them
            if (e.getMessage().startsWith("UNIQUE")) {

                		/*
		 * Build a query with the correct amount of ?,?,?
		 */
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append(ItemColumns.URL + " IN (");
                for (int i = 1; i <= urlArray.length; i++) {
                    queryBuilder.append("\"" + urlArray[i-1] + "\"");
                    if (i != urlArray.length)
                        queryBuilder.append(", ");
                }
                queryBuilder.append(")");
                String where = queryBuilder.toString();

                contentResolver.delete(ItemColumns.URI, where, null);
                int rowsInserted = contentResolver.bulkInsert(ItemColumns.URI, contentValuesArray);
                return;
            }

            return;
        }
		
	}

}
