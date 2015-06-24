package org.bottiger.podcast;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.PlayerService;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

public abstract class AbstractEpisodeFragment extends PodcastBaseFragment {

	protected OnPlaylistRefreshListener mActivityCallback;

	private SharedPreferences prefs;

	String showListenedKey = "showListened";
	Boolean showListenedVal = true;

	private Playlist mPlaylist = null;

	// Container Activity must implement this interface
	// http://developer.android.com/training/basics/fragments/communicating.html
	public interface OnPlaylistRefreshListener {
		public void onRefreshPlaylist();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			mActivityCallback = (OnPlaylistRefreshListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnHeadlineSelectedListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Subscribe
	public void playlistChanged(@NonNull Playlist argPlaylist) {
		mPlaylist = argPlaylist;
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_list, menu);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_bulk_download: {
			Cursor cursor = createCursor(getWhere(), getOrder());
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false) {
				FeedItem feedItem = FeedItem.getByCursor(cursor);
				if (!feedItem.isDownloaded())
					EpisodeDownloadManager.addItemToQueue(feedItem, EpisodeDownloadManager.QUEUE_POSITION.ANYWHERE);

				cursor.moveToNext();
			}
			EpisodeDownloadManager.startDownload(getActivity());
			return true;
		}
		case R.id.menu_clear_playlist: {
			//resetPlaylist(getActivity());

            mPlaylist.resetPlaylist(null);
            //Playlist.resetOrder();
            int size = mPlaylist.defaultSize();
            if (!mPlaylist.isEmpty()) {
				mPlaylist.populatePlaylist(size, true);
                mAdapter.notifyDataSetChanged();
            }
            break;
			//refreshView();
		}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Deprecated
	protected Cursor createCursor(String condition, String order) {
		// return new CursorLoader(getActivity(), ItemColumns.URI, PROJECTION,
		// condition, null, getOrder()).loadInBackground();
		return new CursorLoader(getActivity(), ItemColumns.URI,
				ItemColumns.ALL_COLUMNS, condition, null, order)
				.loadInBackground();
	}

	@Deprecated
	public String getWhere() {
		Boolean showListened = sharedPreferences.getBoolean(showListenedKey,
				showListenedVal);
		String where = (showListened) ? "1" : ItemColumns.LISTENED + "== 0";
		return where;
	}

	@Deprecated
	public String getOrder() {
		return getOrder("DESC", 100);
	}

	@Deprecated
	public static String getOrder(String inputOrder, Integer amount) {
		assert inputOrder != null;

		PlayerService playerService = MainActivity.sBoundPlayerService;

		String playingFirst = "";
		if (playerService != null && playerService.getCurrentItem() != null && playerService.getCurrentItem() instanceof FeedItem) {
			playingFirst = "case " + ItemColumns._ID + " when "
            + ((FeedItem)playerService.getCurrentItem()).getId()
					+ " then 1 else 2 end, ";
		}
		String prioritiesSecond = "case " + ItemColumns.PRIORITY
				+ " when 0 then 2 else 1 end, " + ItemColumns.PRIORITY + ", ";
		String order = playingFirst + prioritiesSecond + ItemColumns.DATE + " "
				+ inputOrder + " LIMIT " + amount; // before:
		return order;
	}

	@Deprecated
	protected static void resetPlaylist(Context context) {
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
		//dbHelper.executeSQL(context, sql, mAdapter);
	}

}
