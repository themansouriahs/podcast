package org.bottiger.podcast.playlist;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;

public class PlaylistCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 1;
	
	private Playlist mPlaylist;
	private Context mContext;

	public PlaylistCursorLoader(Playlist playlist, Fragment fragment, CursorAdapter adapter) {
		super(fragment, adapter);
		mContext = fragment.getActivity();
		loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, playlist.getWhere(), playlist.getOrder());
	}

	@Override
	public void cursorPostProsessing(ReorderCursor cursor) {
		mPlaylist = new Playlist(mContext);
		cursor.moveToPosition(-1); // Set the cursor to start before the first row
		while (cursor.moveToNext()) {
			mPlaylist.setItem(cursor);
		}
	}

}
