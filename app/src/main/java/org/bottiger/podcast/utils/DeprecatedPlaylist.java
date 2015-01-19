package org.bottiger.podcast.utils;

import java.util.PriorityQueue;

import org.bottiger.podcast.provider.FeedItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

@Deprecated
public class DeprecatedPlaylist {

	/*
	private final static int playlistSize = 10;
	private final static int playlistTreshold = 5;

	private static int previousID = -1;
	
	private static SharedPreferences sharedPreferences;
	private static PriorityQueue<QueueItem> playlist = new PriorityQueue<QueueItem>();

	public Playlist(Context context) {
		//getLoaderManager().initLoader(TUTORIAL_LIST_LOADER, null, this);
	}

	public static boolean updatePosition(FeedItem item, int position) {
		QueueItem queueItem = new QueueItem(item, position);
		removeEpisode(item);
		playlist.add(queueItem);
		return true;
	}

	/**
	 * Remove the FeedItem from the playlist.
	 * 
	 * @param item
	 *            the Item to be removed
	 
	public static void removeEpisode(FeedItem item) {
		QueueItem queueItem = new QueueItem(item, 0);
		if (playlist.contains(queueItem))
			playlist.remove(queueItem);
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
	 
	public FeedItem getNext() {
		// String where = getWhere();
		// if (playerService != null) {
		// FeedItem currentItem = playerService.getCurrentItem();
		// if (currentItem != null) {
		// where = where + " AND " + ItemColumns._ID + "<>"
		// + currentItem.getId();
		//
		// }
		// }
		// Cursor nextCursor = new CursorLoader(mContext, ItemColumns.URI,
		// ItemColumns.ALL_COLUMNS, where, null, getOrder("DESC", 1))
		// .loadInBackground();
		// return FeedItem.getByCursor(nextCursor);
		return null;
	}

	public static void setPrevious(int id) {
		previousID = id;
	}
	
	public static int getPrevious() {
		return previousID;
	}

	public static int size() {
		return playlist.size();
	}

	private static void repopulateQueue() {

	}

	public class PlaylistLoader implements
			LoaderManager.LoaderCallbacks<Cursor> {
		// ... existing code
		// LoaderManager.LoaderCallbacks<Cursor> methods:
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return null;
			// TBD
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			// TBD
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// TBD
		}
	}

	/**
	 * Private class for holding the an episodeID and a position in the playlist
	 
	private static class QueueItem implements Comparable<QueueItem> {

		private Integer episodeId;
		private Integer position;
		private Integer priority;
		private String date;

		public QueueItem(FeedItem item, int position) {
			super();
			this.episodeId = Integer.valueOf((int) item.getId());
			this.position = Integer.valueOf(position);
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
		
		public Integer getPlaybackPositionMs() {
			return position;
		}

		@Override
		public int compareTo(QueueItem otherItem) {
			return position.compareTo(otherItem.getPlaybackPositionMs());
			
			// If either is greater than zero return the largest
			//if (priority > 0 || otherItem.getPriority() > 0)
			//	return priority.compareTo(otherItem.getPriority());

			// Otherwize the must be sorted by date.
			//return date.compareTo(otherItem.date);
			
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
	*/
}
