package org.bottiger.podcast;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.ReorderCursor;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PodcastLog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.widget.SeekBar;
import android.widget.TextView;

public abstract class PodcastBaseFragment extends Fragment {

    protected RecyclerView.Adapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected SharedPreferences sharedPreferences;


	protected ReorderCursor mCursor = null;

	private static SeekBar mProgressBar = null;

	public SeekBar getProgress() {
		return mProgressBar;
	}


	// Container Activity must implement this interface
	public interface OnItemSelectedListener {
		public void onItemSelected(long id);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onResume() {
		super.onResume();
	}


	public ReorderCursor getCursor() {
		return this.mCursor;
	}

	abstract String getWhere();

	abstract String getOrder();

}
