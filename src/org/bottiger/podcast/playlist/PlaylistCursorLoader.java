package org.bottiger.podcast.playlist;

import org.bottiger.podcast.provider.ItemColumns;

import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;

public class PlaylistCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 1;

	public PlaylistCursorLoader(Playlist playlist, Fragment fragment, CursorAdapter adapter) {
		super(fragment, adapter);
		
		loadCursor(LOADER_ID, ItemColumns.URI, ItemColumns.ALL_COLUMNS, playlist.getWhere(), playlist.getOrder());
	}

}
