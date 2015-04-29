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

    protected RecyclerView currentView;
    protected RecyclerView.Adapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected SharedPreferences sharedPreferences;

	protected CursorAdapter mCursorAdapter;

	protected final PodcastLog log = PodcastLog.getLog(getClass());

    private Playlist mPlaylist;

	protected ReorderCursor mCursor = null;

	private static TextView mCurrentTime = null;
	private static SeekBar mProgressBar = null;
	private static TextView mDuration = null;

	public TextView getCurrentTime() {
		return mCurrentTime;
	}

	public static void setCurrentTime(TextView mCurrentTime) {
		PodcastBaseFragment.mCurrentTime = mCurrentTime;
	}

	public SeekBar getProgress() {
		return mProgressBar;
	}

	public void setProgressBar(SeekBar mProgress) {
		PodcastBaseFragment.mProgressBar = mProgress;
	}

	public void setDuration(TextView mDuration) {
		PodcastBaseFragment.mDuration = mDuration;
	}

    public RecyclerView getListView() {
        return currentView;
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

        mPlaylist = getPlaylist();
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

	public void refreshView() {
		FeedItem.clearCache();
	}

    @Deprecated
    public Playlist getPlaylist() {
        return PlayerService.getPlaylist(null);
    }

	abstract String getWhere();

	abstract String getOrder();

	protected static String orderByFirst(String condition) {
		String priorityOrder = "case when " + condition + " then 1 else 2 end";
		return priorityOrder;
	}

    public static PlayerService getPlayerService() {
        if (MainActivity.sBoundPlayerService == null) {
            throw new IllegalStateException("What should we do here?");
        }
        return MainActivity.sBoundPlayerService;
    }
}
