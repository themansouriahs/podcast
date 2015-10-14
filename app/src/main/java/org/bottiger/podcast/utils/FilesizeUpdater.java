package org.bottiger.podcast.utils;


import java.util.HashMap;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;

import android.content.Context;
import android.widget.TextView;

/*
 * Updates the current filesize of a file while being downloaded
 */
public class FilesizeUpdater {

	private static HashMap<Long, TextView> viewTable = new HashMap<Long, TextView>();
	
	public static void put(Context context, long itemID, TextView tv) {
		//FeedItem item = FeedItem.getById(context.getContentResolver(), itemID);
		FeedItem item = SoundWaves.getLibraryInstance().getEpisode(itemID);
		viewTable.put(item.id, tv);
	}
	
	public static void put(FeedItem item, TextView tv) {
		viewTable.put(item.id, tv);
	}
	
	public static TextView get(FeedItem item) {
		TextView tv = viewTable.get(item.id);
		return tv;
	}
}
