package info.bottiger.podcast;

import info.bottiger.podcast.adapters.CompactListCursorAdapter;
import info.bottiger.podcast.adapters.ItemCursorAdapter;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
public class FeedFragment extends AbstractEpisodeFragment {

	// FIXME should not be static
	private static long subId = 14;
	private ViewGroup header;
	
	public static CompactListCursorAdapter listItemCursorAdapter(Context context,
			PodcastBaseFragment fragment, Cursor cursor) {
		CompactListCursorAdapter.FieldHandler[] fields = {
				CompactListCursorAdapter.defaultTextFieldHandler,
				new CompactListCursorAdapter.TextFieldHandler(), new ItemCursorAdapter.TextFieldHandler(),
				new CompactListCursorAdapter.IconFieldHandler(mIconMap),
		};
		return new CompactListCursorAdapter(context, R.layout.episode_list_compact,
				cursor, new String[] { ItemColumns.TITLE,
						ItemColumns.SUB_TITLE, ItemColumns.DURATION,
						ItemColumns.IMAGE_URL }, new int[] { R.id.title,
						R.id.podcast, R.id.duration, R.id.list_image }, fields);
	}
	
	public SimpleCursorAdapter getAdapter(Cursor cursor) {
		if (mAdapter != null)
			return mAdapter;
		
		return listItemCursorAdapter(this.getActivity(),
				this, cursor);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		header = (ViewGroup) inflater.inflate(R.layout.podcast_header, null);
		Subscription subscription = getSubscription(getActivity().getContentResolver());
		
		if (subscription != null)
			setHeader(header, subscription);

		
		//fragmentView = inflater.inflate(R.layout.subscription_list, container, false);
		fragmentView = inflater.inflate(R.layout.recent_new, container, false);
		Intent intent = getActivity().getIntent();
		intent.setData(ItemColumns.URI);

		getPref();
		return fragmentView;
	}
	
	private void setHeader(ViewGroup header, Subscription subscription) {
		
		TextView title = (TextView) header.findViewById(R.id.title);
		ImageView icon = (ImageView) header.findViewById(R.id.podcast_cover);
		
		title.setText(subscription.title);
		ImageLoader imageLoader = ((CompactListCursorAdapter)getAdapter(mCursor)).getImageLoader(getActivity());
 
		if (subscription.imageURL != null && !subscription.imageURL.equals("")) {
			imageLoader.displayImage(subscription.imageURL, icon);
		}		
	}
	
	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		
		if (header != null)  this.getListView().addHeaderView(header);


		mAdapter = FeedFragment.listItemCursorAdapter(this.getActivity(),
				this, mCursor);
		startInit(1, ItemColumns.URI, ItemColumns.ALL_COLUMNS, getWhere(), getOrder());

		/*
		final PullToRefreshListView pullToRefreshView = (PullToRefreshListView) fragmentView
				.findViewById(R.id.episode_list_compact);

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
	
	public static FeedFragment newInstance(long subID) {
	    FeedFragment fragment = new FeedFragment();

	    Bundle args = new Bundle();
	    args.putLong("subID", subID);
	    fragment.setArguments(args);
	    
	    FeedFragment.subId = subID;

	    return fragment;
	}


	@Override
	public String getWhere() {		
		String where = ItemColumns.SUBS_ID + "=" + subId;
		return where;
	}

	public void showEpisodes(String condition) {
		mCursor = createCursor(condition, getOrder());

		mAdapter = FeedFragment.listItemCursorAdapter(this.getActivity(),
				this, mCursor);

		setListAdapter(mAdapter);
	}
	
	private Subscription getSubscription(ContentResolver contentResolver) {
		return Subscription.getById(contentResolver, subId);
	}
}
