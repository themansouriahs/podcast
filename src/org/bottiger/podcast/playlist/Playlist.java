package org.bottiger.podcast.playlist;

import java.util.concurrent.CopyOnWriteArrayList;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PodcastOpenHelper;
import org.bottiger.podcast.service.PlayerService;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;

public class Playlist {
	
	private int MAX_SIZE = 20;

	private Context mContext;

	private static CopyOnWriteArrayList<FeedItem> mPlaylist =  new CopyOnWriteArrayList<FeedItem>();
	private SharedPreferences sharedPreferences;

	// Shared setting key/values
	private String showListenedKey = "showListened";
	private Boolean showListenedVal = true;
	private String inputOrderKey = "inputOrder";
	private String defaultOrder = "DESC";
	private String amountKey = "amountOfEpisodes";
	private int amountValue = 20;

	public Playlist(Context context, int length) {
		this(context);
		this.populatePlaylist(length);
	}
	
	public Playlist(Context context) {
		this.mContext = context;
		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
	}

	public void getPlaylistCursor(CursorAdapter adapter) {
	}

	/**
	 * @return The playlist as a list of episodes
	 */
	public CopyOnWriteArrayList<FeedItem> getPlaylist() {
		return mPlaylist;
	}
	
	/**
	 * 
	 * @param position in the playlist (0-indexed)
	 * @return The episode at the given position
	 */
	public FeedItem getItem(int position) {
			return mPlaylist.get(position);
	}
	
	/**
	 * 
	 * @param position
	 * @param item
	 */
	public void setItem(int position, FeedItem item) {
		int size = mPlaylist.size(); 
		if (size > position)
			mPlaylist.set(position, item);
		else if (size == position){
			mPlaylist.add(item);
		}
	}
	
	/**
	 * 
	 * @param cursor
	 */
	public void setItem(Cursor cursor) {
		int position = cursor.getPosition();
		if (position < MAX_SIZE) {
			setItem(position, FeedItem.getByCursor(cursor));
		}
	}

	/**
	 * @return The next item in the playlist
	 */
	public FeedItem nextEpisode() {
		if (mPlaylist.size() > 1)
			return mPlaylist.get(1);
		return null;
	}

	/**
	 * 
	 * 
	 * @param inputOrder
	 * @param Amount
	 *            of episodes
	 * @return A SQL formatted string of the order
	 */
	public String getOrder() {

		String inputOrder = sharedPreferences.getString(inputOrderKey,
				defaultOrder);
		int amount = sharedPreferences.getInt(amountKey,
				amountValue);

		PlayerService playerService = PodcastBaseFragment.mPlayerServiceBinder;

		String playingFirst = "";
		if (playerService != null && playerService.getCurrentItem() != null) {
			playingFirst = "case " + ItemColumns._ID + " when "
					+ playerService.getCurrentItem().getId()
					+ " then 1 else 2 end, ";
		}
		String prioritiesSecond = "case " + ItemColumns.PRIORITY
				+ " when 0 then 2 else 1 end, " + ItemColumns.PRIORITY + ", ";
		String order = playingFirst + prioritiesSecond + ItemColumns.DATE + " "
				+ inputOrder + " LIMIT " + amount; // before:
		return order;
	}

	/**
	 * 
	 * @return A SQL formatted string of the where clause
	 */
	public String getWhere() {
		Boolean showListened = sharedPreferences.getBoolean(showListenedKey,
				showListenedVal);
		String where = (showListened) ? "1" : ItemColumns.LISTENED + "== 0";
		return where;
	}

	/**
	 * 
	 */
	public void resetPlaylist(CursorAdapter adapter) {
		// Update the database
		String currentTime = String.valueOf(System.currentTimeMillis());
		String updateLastUpdate = ", " + ItemColumns.LAST_UPDATE + "="
				+ currentTime + " ";

		// We remove the playlist position for all items in the playlist.
		String action = "UPDATE " + ItemColumns.TABLE_NAME + " SET ";
		String value = ItemColumns.PRIORITY + "=0" + updateLastUpdate;
		String where = "WHERE " + ItemColumns.PRIORITY + "<> 0";

		// Also update the timestamp of the top item in order to indicate to the
		// drivesyncer
		// Our data is up tp date.
		String where2 = " OR " + ItemColumns._ID + "==(select "
				+ ItemColumns._ID + " from " + ItemColumns.TABLE_NAME
				+ " order by " + ItemColumns.DATE + " desc limit 1)";

		String sql = action + value + where + where2;

		DatabaseHelper dbHelper = new DatabaseHelper();
		dbHelper.executeSQL(mContext, sql, adapter);
	}
	
	/**
	 * Populates the playlist up to a certain length
	 * 
	 * @param length of the playlist
	 */
	private void populatePlaylist(int length) {
		if (mPlaylist.size() >= length)
			return;
		
		PodcastOpenHelper helper = new PodcastOpenHelper(mContext);
		SQLiteDatabase database = helper.getWritableDatabase();
		Cursor cursor = database.query(ItemColumns.TABLE_NAME, ItemColumns.ALL_COLUMNS, getWhere(), null, null, null, getOrder());
		
		mPlaylist.clear();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			setItem(cursor);
		}
	}

}
