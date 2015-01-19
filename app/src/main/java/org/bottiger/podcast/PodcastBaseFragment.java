package org.bottiger.podcast;

import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.ReorderCursor;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.StrUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/* Copy of PodcastBaseActivity */
public abstract class PodcastBaseFragment extends Fragment {

	public static final int COLUMN_INDEX_TITLE = 1;

	protected Fragment fragmentView;

    protected RecyclerView currentView;
    protected RecyclerView.Adapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected SharedPreferences sharedPreferences;

	// protected static PodcastService mServiceBinder = null;
	public static PlayerService mPlayerServiceBinder = null;
	protected static ComponentName mService = null;
	// protected final Log log = Log.getLog(getClass());

	protected CursorAdapter mCursorAdapter;
	// protected Cursor mCursor = null;

	// protected boolean mInit = false;
	protected Intent mPrevIntent = null;

	protected Intent mNextIntent = null;

	OnItemSelectedListener mListener;

	protected final PodcastLog log = PodcastLog.getLog(getClass());

	private long mLastSeekEventTime;
	private boolean mFromTouch;

	public static final int REFRESH = 1;
	public static final int PLAYITEM = 2;
	public static final int UPDATE_FILESIZE = 3;

    private Playlist mPlaylist;

	protected ReorderCursor mCursor = null;

	private static TextView mCurrentTime = null;
	private static SeekBar mProgressBar = null;
	private static TextView mDuration = null;

	private int contextMenuViewID;

	protected abstract int getItemLayout();

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

	public ServiceConnection playerServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayerServiceBinder = ((PlayerService.PlayerBinder) service)
					.getService();
			PlayerStatusObservable.setActivity(getActivity());
			// log.debug("onServiceConnected");
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mPlayerServiceBinder = null;
			// log.debug("onServiceDisconnected");
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnItemSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnArticleSelectedListener");
		}
	}

    public RecyclerView getListView() {
        return currentView;
    }

	Subscription getSubscription(Object o) {
		Cursor item = (Cursor) o;
		Long id = item.getLong(item.getColumnIndex(BaseColumns._ID));
		new Subscription();
		return Subscription.getById(getActivity().getContentResolver(), id);
	}

	// Container Activity must implement this interface
	public interface OnItemSelectedListener {
		public void onItemSelected(long id);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mService = getActivity().startService(
				new Intent(getActivity(), PlayerService.class));

		Intent bindIntent = new Intent(getActivity(), PlayerService.class);
		getActivity().bindService(bindIntent, playerServiceConnection,
				Context.BIND_AUTO_CREATE);

		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

        mPlaylist = new Playlist(getActivity(), 30, true);
	}

	abstract View getPullView();

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			// unbindService(playerServiceConnection);
			getActivity().unbindService(playerServiceConnection);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	//@Override
	public void onRefreshStarted(View view) {
		/**
		 * Simulate Refresh with 4 seconds sleep
		 */
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					PodcastDownloadManager.start_update(getActivity());
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				// Notify PullToRefreshAttacher that the refresh has finished
				//getPullToRefreshAttacher().setRefreshComplete();
			}
		}.execute();
	}


	public ReorderCursor getCursor() {
		return this.mCursor;
	}

	public void refreshView() {
		FeedItem.clearCache();
	}

    public Playlist getPlaylist() {
        return mPlaylist;
    }

	abstract String getWhere();

	abstract String getOrder();

	protected static String orderByFirst(String condition) {
		String priorityOrder = "case when " + condition + " then 1 else 2 end";
		return priorityOrder;
	}
}
