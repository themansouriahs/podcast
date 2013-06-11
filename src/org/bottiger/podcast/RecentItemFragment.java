package org.bottiger.podcast;



import java.util.HashMap;

import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.utils.ControlButtons;
import org.bottiger.podcast.utils.ExpandAnimation;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

public class RecentItemFragment extends PlaylistDSLVFragment {

	public static HashMap<Integer, Integer> mKeepIconMap;

	private long mCurCheckID = -1;
	boolean mDualPane;
	
	
    public static RecentItemFragment newInstance(int headers, int footers) {
    	RecentItemFragment f = new RecentItemFragment();

        Bundle args = new Bundle();
        args.putInt("headers", headers);
        args.putInt("footers", footers);
        f.setArguments(args);

        return f;
    }

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
		mAdapter = this.getAdapter(mCursor); // listItemCursorAdapter(this.getActivity(),this, mCursor);
		startInit(1, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());		

		//setEmptyText("No podcasts :( You should add some feeds and refresh the list");

		if (mDualPane) {
			// In dual-pane mode, the list view highlights the selected item.
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
			// Make sure our UI is in the correct state.
			// showDetails(mCurCheckPosition);
		}

		// FIXME
		//enablePullToRefresh();
		
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("curChoice", mCurCheckID);
	}

	public static CursorAdapter listItemCursorAdapter(Context context,
			PodcastBaseFragment fragment, Cursor cursor) {
		ItemCursorAdapter.FieldHandler[] fields = {
				ItemCursorAdapter.defaultTextFieldHandler,
				new ItemCursorAdapter.TextFieldHandler(),
				new ItemCursorAdapter.TextFieldHandler(),
				new ItemCursorAdapter.IconFieldHandler(mIconMap), };
		return new ItemCursorAdapter(context, fragment, R.layout.episode_list,
				cursor, new String[] { ItemColumns.TITLE,
						ItemColumns.SUB_TITLE, ItemColumns.DURATION,
						ItemColumns.IMAGE_URL }, new int[] { R.id.title,
						R.id.podcast, R.id.duration, R.id.list_image }, fields);
	}
	
	public void setAdapter(CursorAdapter adapter) {
		mAdapter = adapter;
	}
	
	public CursorAdapter getAdapter(Cursor cursor) {
		if (mAdapter != null)
			return mAdapter;
		
		CursorAdapter adapter = listItemCursorAdapter(this.getActivity(),this, cursor);
		setAdapter(adapter);
		return adapter;
		//return listItemCursorAdapter(this.getActivity(),
		//		this, cursor);
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

	/*
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		layoutID = R.layout.recent_new;
		fragmentView = inflater.inflate(layoutID, container, false);
		Intent intent = getActivity().getIntent();
		intent.setData(ItemColumns.URI);
		
		
		getPref();
		return fragmentView;
	}
	*/

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor item = (Cursor) l.getItemAtPosition(position); // https://github.com/chrisbanes/Android-PullToRefresh/issues/99
		
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
		mCursor = createCursor(condition, getOrder());

		mAdapter = RecentItemFragment.listItemCursorAdapter(this.getActivity(),
				this, mCursor);

		if (this.mCurCheckID > 0) {
			((ItemCursorAdapter) mAdapter).showItem(mCurCheckID);
			// View view = getViewByID(mCurCheckID);
			// this.setPlayerListeners(view, mCurCheckID);
		}

		setListAdapter(mAdapter);
	}

	private void togglePlayer(ListView list, Cursor item) {
		int start = list.getFirstVisiblePosition();
		boolean setListners = false;

		((ItemCursorAdapter) mAdapter).toggleItem(item);
		long id = item.getLong(item.getColumnIndex(BaseColumns._ID));
		String duration = item.getString(item
				.getColumnIndex(ItemColumns.DURATION));
		int position = item.getPosition();
		
		View view1 = list.getChildAt(0);
		View view2 = list.getChildAt(1);
		View view3 = list.getChildAt(2);
		
		int newStart = (start > 0) ? start+1 : start;
		
		View view = list.getChildAt(position - start);
		mAdapter.notifyDataSetChanged();

		TextView title = (TextView) view
				.findViewById(R.id.title);
		CharSequence title2 = title.getText();
		
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
		ControlButtons.setPlayerListeners(list, view, id);

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
	public void setListAdapter() {
		// TODO Auto-generated method stub
		CursorAdapter sca = getAdapter(getCursor()); 
		setListAdapter(sca);
		
	}

	@Override
	View getPullView() {
		return getListView();
	}
}
