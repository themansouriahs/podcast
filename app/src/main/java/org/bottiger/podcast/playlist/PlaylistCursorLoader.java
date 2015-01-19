package org.bottiger.podcast.playlist;

import android.database.Cursor;
import android.support.v4.app.Fragment;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.provider.ItemColumns;

public class PlaylistCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 1;
	
	private Playlist mPlaylist;

	public PlaylistCursorLoader(Playlist playlist, Fragment fragment, AbstractPodcastAdapter adapter, Cursor cursor) {
		super(fragment, adapter, cursor);
		mPlaylist = playlist;
		//loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, playlist.getWhere(), playlist.getOrder());
		requery();
	}
	
	public void requery() {
		loadCursor(LOADER_ID, ItemColumns.PLAYLIST_URI, ItemColumns.ALL_COLUMNS, mPlaylist.getWhere(), mPlaylist.getOrder());
	}

    /*
	@Override
	public ReorderCursor getReorderCursor(Cursor cursor) {
		return new ReorderCursor(mFragment.getActivity(), cursor, mPlaylist.getReordering().clone());
	}

	@Override
	public void cursorPostProsessing(ReorderCursor cursor) {
		cursor.moveToPosition(-1); // Set the cursor to start before the first row
		while (cursor.moveToNext()) {
			mPlaylist.setItem(cursor);
		}
		mPlaylist.unlock();
	}*/

}
