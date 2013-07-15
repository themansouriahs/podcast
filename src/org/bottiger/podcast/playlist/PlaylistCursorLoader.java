package org.bottiger.podcast.playlist;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;

public class PlaylistCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 1;
	
	private Playlist mPlaylist;

	public PlaylistCursorLoader(Playlist playlist, Fragment fragment, CursorAdapter adapter) {
		super(fragment, adapter);
		mPlaylist = playlist;
		loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, playlist.getWhere(), playlist.getOrder());
	}
	
	@Override
	public ReorderCursor getReorderCursor(Cursor cursor) {
		return new ReorderCursor(mFragment.getActivity(), mAdapter,
				cursor, mPlaylist.getReordering());
	}

	@Override
	public void cursorPostProsessing(ReorderCursor cursor) {
		cursor.moveToPosition(-1); // Set the cursor to start before the first row
		while (cursor.moveToNext()) {
			mPlaylist.setItem(cursor);
		}
	}

}
