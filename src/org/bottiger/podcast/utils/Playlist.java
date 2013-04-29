package org.bottiger.podcast.utils;


import java.util.PriorityQueue;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.PlayerService;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.LruCache;

public class Playlist {

	private static PlaylistLruCache cache = null;
	private static SharedPreferences sharedPreferences;
	
	private static PriorityQueue<QueueItem> playlist = new PriorityQueue<QueueItem>();

	public Playlist(Context context) {
	}
	
	public static boolean updatePosition(FeedItem item) {
		QueueItem queueItem = new QueueItem(item);
		if (playlist.contains(queueItem)) 
			playlist.remove(queueItem);
		playlist.add(queueItem);
		return true;
	}
	
	public static void resetPlaylist() {
		playlist.clear();
	}
	
	public static Integer currentId() {
		return playlist.peek().getId();
	}
	
	public static Integer nextId() {
		if (playlist.size() < 2)
			return -1;
		
		QueueItem currentItem = playlist.poll();
		QueueItem nextItem = playlist.peek();
		
		// put the first item back
		playlist.add(currentItem);
		
		return nextItem.getId();
	}
	
	public static boolean removeHead() {
		if (playlist.isEmpty())
			return false;
		playlist.poll();
		return true;
	}

	/**
	 * @return the next item to be played
	 */
	public FeedItem getNext() {
//		String where = getWhere();
//		if (playerService != null) {
//			FeedItem currentItem = playerService.getCurrentItem();
//			if (currentItem != null) {
//				where = where + " AND " + ItemColumns._ID + "<>"
//						+ currentItem.getId();
//
//			}
//		}
//		Cursor nextCursor = new CursorLoader(mContext, ItemColumns.URI,
//				ItemColumns.ALL_COLUMNS, where, null, getOrder("DESC", 1))
//				.loadInBackground();
//		return FeedItem.getByCursor(nextCursor);
		return null;
	}

	public static FeedItem getPrevious() {
		return null;
	}

	public static int size() {
		return playlist.size();
	}
	
	/**
	 * Private class for holding the an episodeID and a position in the playlist
	 */
	private static class QueueItem implements Comparable<QueueItem> {
		
		private Integer episodeId;
		private Integer priority;
		private String date;
		
		public QueueItem(FeedItem item) {
			super();
			this.episodeId = Integer.valueOf((int)item.getId());
			this.priority = Integer.valueOf(item.getPriority());
			this.date = item.getDate();
		}
		
		public Integer getId() {
			return episodeId;
		}
		
		public Integer getPriority() {
			return priority;
		}
		
		public String getDate() {
			return date;
		}

		@Override	
		public int compareTo(QueueItem otherItem) {
			// If either is greater than zero return the largest
			if (priority > 0 || otherItem.getPriority() > 0)
				return priority.compareTo(otherItem.getPriority());
			
			// Otherwize the must be sorted by date.
			return date.compareTo(otherItem.date);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((episodeId == null) ? 0 : episodeId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueueItem other = (QueueItem) obj;
			if (episodeId == null) {
				if (other.episodeId != null)
					return false;
			} else if (!episodeId.equals(other.episodeId))
				return false;
			return true;
		}
	}
	
	/**
	 * Caching class for keeping items in memory
	 */
	private static class PlaylistLruCache extends LruCache<Long, QueueItem> {

		public PlaylistLruCache(int maxSize) {
			super(maxSize);
		}

	}
}
