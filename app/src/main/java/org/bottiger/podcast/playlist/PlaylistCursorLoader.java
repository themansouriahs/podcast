package org.bottiger.podcast.playlist;

import android.app.Activity;
import android.database.Cursor;
import android.support.v4.app.Fragment;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.adapters.FeedViewAdapter;
import org.bottiger.podcast.provider.ItemColumns;

public class PlaylistCursorLoader extends GenericCursorLoader {
	
	private final int LOADER_ID = 1;
	
	private Playlist mPlaylist;

    public PlaylistCursorLoader(Fragment fragment, FeedViewAdapter adapter, Cursor cursor) {
        super(fragment, adapter, cursor);
    }

    public PlaylistCursorLoader(Activity activity, FeedViewAdapter adapter, Cursor cursor) {
        super(activity, adapter, cursor);
    }

    /*
	public PlaylistCursorLoader(Playlist playlist, Fragment fragment, AbstractPodcastAdapter adapter, Cursor cursor) {
		super(fragment, adapter, cursor);
		mPlaylist = playlist;
		requery();
	}*/
	
	public void requery() {
		loadCursor(LOADER_ID, ItemColumns.PLAYLIST_URI, ItemColumns.ALL_COLUMNS, mPlaylist.getWhere(), mPlaylist.getOrder());
	}
}
