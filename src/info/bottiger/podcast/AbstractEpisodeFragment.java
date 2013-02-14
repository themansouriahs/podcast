package info.bottiger.podcast;

import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.utils.ControlButtons;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public abstract class AbstractEpisodeFragment extends PodcastBaseFragment {
	
	protected static HashMap<Integer, Integer> mIconMap;
	
	protected long pref_order;
	protected long pref_where;
	protected long pref_select;
	

	protected ListView actualListView = null;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.episode_list, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_download_all: {
			Cursor cursor = createCursor(getWhere());
			while (cursor.moveToNext()) {
				FeedItem feedItem = FeedItem.getByCursor(cursor);
				if (!feedItem.isDownloaded())
					PodcastDownloadManager.addItemToQueue(feedItem);
				PodcastDownloadManager.startDownload(this.getActivity());
				return true;
			}
		}
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

	public String getWhere() {
		String where = "";
		return where;
	}
	
	@Override
	public void startInit() {
		showEpisodes(getWhere());
	}
	
	public void showEpisodes(String condition) {
		mCursor = createCursor(condition);

		mAdapter = RecentItemFragment.listItemCursorAdapter(this.getActivity(),
				this, mCursor);

		setListAdapter(mAdapter);
	}
	
	public void getPref() {
		SharedPreferences pref = getActivity().getSharedPreferences(
				SettingsActivity.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
		pref_order = pref.getLong("pref_order", 2);
		pref_where = pref.getLong("pref_where", 0);
		pref_select = pref.getLong("pref_select", 0);
	}
	
	protected Cursor createCursor(String condition) {
		// return new CursorLoader(getActivity(), ItemColumns.URI, PROJECTION,
		// condition, null, getOrder()).loadInBackground();
		return new CursorLoader(getActivity(), ItemColumns.URI,
				ItemColumns.ALL_COLUMNS, condition, null, getOrder())
				.loadInBackground();
	}
	
	public String getOrder() {
		String order = ItemColumns.DATE + " DESC LIMIT 20"; // before:
		// ItemColumns.CREATED
		if (pref_order == 0) {
			order = ItemColumns.SUBS_ID + "," + order;
		} else if (pref_order == 1) {
			order = ItemColumns.STATUS + "," + order;
		}
		return order;
	}

}
