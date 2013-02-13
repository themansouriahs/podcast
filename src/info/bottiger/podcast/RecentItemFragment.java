package info.bottiger.podcast;

import info.bottiger.podcast.adapters.ItemCursorAdapter;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.utils.ControlButtons;
import info.bottiger.podcast.utils.ExpandAnimation;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class RecentItemFragment extends AbstractEpisodeFragment {

	private static final String[] PROJECTION = new String[] { ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.SUB_TITLE, //
			ItemColumns.IMAGE_URL, //
			ItemColumns.DURATION, //
			ItemColumns.STATUS, //
			ItemColumns.SUBS_ID, //
			ItemColumns.FILESIZE, ItemColumns.PATHNAME, //
			ItemColumns.OFFSET, //
			ItemColumns.LISTENED //

	};

	public static HashMap<Integer, Integer> mKeepIconMap;

	private View mCurrentPlayer = null;

	//private ItemCursorAdapter mAdapter;


	private long mCurCheckID = -1;
	boolean mDualPane;

	
	public SimpleCursorAdapter getAdapter(Cursor cursor) {
		return RecentItemFragment.listItemCursorAdapter(this.getActivity(),
				this, cursor, mListItemView);
	}

	
	int mListItemView;
	int mListView;

	
	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (savedInstanceState != null) {
			// Restore last state for checked position.
			mCurCheckID = savedInstanceState.getLong("curChoice", 0);
		}

		// Populate list with our static array of titles.
		//startInit();
		mAdapter = RecentItemFragment.listItemCursorAdapter(this.getActivity(),
				this, mCursor, mListItemView);
		startInit(1, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());

		if (mDualPane) {
			// In dual-pane mode, the list view highlights the selected item.
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
			// Make sure our UI is in the correct state.
			// showDetails(mCurCheckPosition);
		}

		
		/*
		final PullToRefreshListView pullToRefreshView = (PullToRefreshListView) fragmentView
				.findViewById(mListView);

		OnRefreshListener<ListView> pullToRefreshListener = new OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				// SwipeActivity.mPodcastServiceBinder.start_update(pullToRefreshView);
				PodcastService.start_update(getActivity(), pullToRefreshView);
			}
		};

		
		actualListView = pullToRefreshView.getRefreshableView();
		pullToRefreshView.getLoadingLayoutProxy().setRefreshingLabel(
				"Refreshing feeds");
		pullToRefreshView.setOnRefreshListener(pullToRefreshListener);
		*/
		
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("curChoice", mCurCheckID);
	}

	public static ItemCursorAdapter listItemCursorAdapter(Context context,
			PodcastBaseFragment fragment, Cursor cursor, int listItemView) {
		ItemCursorAdapter.FieldHandler[] fields = {
				ItemCursorAdapter.defaultTextFieldHandler,
				new ItemCursorAdapter.TextFieldHandler(),
				new ItemCursorAdapter.TextFieldHandler(),
				new ItemCursorAdapter.IconFieldHandler(mIconMap), };
		return new ItemCursorAdapter(context, fragment, listItemView,
				cursor, new String[] { ItemColumns.TITLE,
						ItemColumns.SUB_TITLE, ItemColumns.DURATION,
						ItemColumns.IMAGE_URL }, new int[] { R.id.title,
						R.id.podcast, R.id.duration, R.id.list_image }, fields);
	}

	@Override
	public void onResume() {
		super.onResume();
		ControlButtons.fragment = this;
		if (mPlayerServiceBinder != null && mPlayerServiceBinder.isPlaying()) {
			long current_id = mPlayerServiceBinder.getCurrentItem().id;
			showPlayingEpisode(current_id);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		int INTERNAL_EMPTY_ID = 0x00ff0001;
		
		mListView = R.layout.recent;
		mListItemView = R.layout.episode_list;
		
	
		fragmentView = inflater.inflate(mListView, container, false);
		
	    (fragmentView.findViewById(R.id.internalEmpty)).setId(INTERNAL_EMPTY_ID);
	    mList = (ListView) fragmentView.findViewById(mListItemView);
		
		
		Intent intent = getActivity().getIntent();
		intent.setData(ItemColumns.URI);
	    mListContainer =  fragmentView.findViewById(R.id.listContainer);
	    mProgressContainer = fragmentView.findViewById(R.id.progressContainer);
	    mListShown = true;

		getPref();
		return fragmentView;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor item = (Cursor) l.getItemAtPosition(position); // FIXME
																// https://github.com/chrisbanes/Android-PullToRefresh/issues/99
		this.togglePlayer(l, item);

	}

	public void showPlayingEpisode(long playingEpisodeID) {
		// this.getActivity().findViewById(id)
		// ViewStub stub = (ViewStub)
		// getActivity().findViewById(R.id.stub_play);
		// View inflated = stub.inflate();

		FeedItem episode = FeedItem.getById(getActivity().getContentResolver(),
				playingEpisodeID);

		// TextView t = (TextView) inflated.findViewById(R.id.player_title);
		// FIXME
		// t.setText(episode.title); FIXME
		listNonPlayingEpisodes(playingEpisodeID);
	}

	public void listNonPlayingEpisodes(long playingEpisodeID) {
		// String excludePLayingEpisode = ItemColumns._ID + "!="
		// + playingEpisodeID;
		String excludePLayingEpisode = "";
		showEpisodes(excludePLayingEpisode);
	}

	public void showEpisodes(String condition) {
		mCursor = createCursor(condition);

		mAdapter = RecentItemFragment.listItemCursorAdapter(this.getActivity(),
				this, mCursor, mListItemView);

		if (this.mCurCheckID > 0) {
			((ItemCursorAdapter)mAdapter).showItem(mCurCheckID);
		}

		setListAdapter(mAdapter);
	}

	private void togglePlayer(ListView list, Cursor item) {
		int start = list.getFirstVisiblePosition();
		boolean setListners = false;

		((ItemCursorAdapter)mAdapter).toggleItem(item);
		long id = item.getLong(item.getColumnIndex(BaseColumns._ID));
		String duration = item.getString(item
				.getColumnIndex(ItemColumns.DURATION));
		int position = item.getPosition();
		View view = list.getChildAt(position - start + 1);
		mAdapter.notifyDataSetChanged();

		ControlButtons.Holder viewHolder = new ControlButtons.Holder();
		viewHolder.currentTime = (TextView) view
				.findViewById(R.id.current_position);
		viewHolder.duration = (TextView) view.findViewById(R.id.duration);
		if (viewHolder.duration != null)
			viewHolder.duration.setText(duration);

		ViewStub stub = (ViewStub) view.findViewById(R.id.stub);
		if (stub != null) {
			stub.inflate();
			ExpandAnimation expandAni = new ExpandAnimation(stub, 5000);
			stub.startAnimation(expandAni);

			setListners = true;
		} else {
			View player = view.findViewById(R.id.stub_player);
			if (player.getVisibility() == View.VISIBLE) {
				player.setVisibility(View.GONE);
				mCurCheckID = -1;
			} else {
				player.setVisibility(View.VISIBLE);
				setListners = true;
			}
		}

		// if (setListners) {
		// setPlayerListeners(view, id);
		// }
		ControlButtons.setPlayerListeners(view, id);

		// updateCurrentPosition(FeedItem.getById(getActivity().getContentResolver(),
		// id));
		updateCurrentPosition();
	}

	private View getViewByID(long id) {

		ListView list = getListView();
		int start = list.getFirstVisiblePosition();

		for (int i = start, j = list.getLastVisiblePosition(); i <= j; i++) {
			Cursor item = (Cursor) list.getItemAtPosition(i);

			if (id == item.getLong(item.getColumnIndex(BaseColumns._ID))) {
				View view = list.getChildAt(i);
				return view;
			}

		}
		return null;
	}


	@Override
	Subscription getSubscription(Object o) {
		Cursor item = (Cursor) o;
		Long id = item.getLong(item.getColumnIndex(ItemColumns.SUBS_ID));
		new Subscription();
		return Subscription.getById(getActivity().getContentResolver(), id);
	}
}
