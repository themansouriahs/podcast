package org.bottiger.podcast;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.adapters.SubscriptionGridCursorAdapter;
import org.bottiger.podcast.adapters.SubscriptionListCursorAdapter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.utils.DialogMenu;
import org.bottiger.podcast.utils.FragmentUtils;

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.dragontek.mygpoclient.pub.PublicClient;
import com.dragontek.mygpoclient.simple.IPodcast;

public class SubscriptionsFragment extends Fragment {

	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;
	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 12;

	private static enum LayoutType {
		LIST, GRID
	};
	
	private static enum ContentType {
		LOCAL, REMOTE
	};

	private FragmentUtils mFragmentUtils;

	TextView addSubscriptionView = null;;
	private static HashMap<Integer, Integer> mIconMap;
	private View fragmentView;

	private GridView mGridView;
	private TextView searchStatus;

	Subscription mChannel = null;
	long id;
	
	private LayoutType displayLayout = LayoutType.GRID;
	private ContentType contentType = ContentType.LOCAL;

	static {
		mIconMap = new HashMap<Integer, Integer>();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	public static boolean channelExists(Activity act, Uri uri) {
		Cursor cursor = act.getContentResolver().query(uri,
				SubscriptionColumns.ALL_COLUMNS, null, null, null);
		if (!cursor.moveToFirst()) {
			return false;
		}

		Subscription ch = Subscription.getByCursor(cursor);

		cursor.close();

		return (ch != null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		fragmentView = inflater.inflate(getLayoutType(), container, false);

		mFragmentUtils = new FragmentUtils(getActivity(), fragmentView, this);
		mGridView = (GridView) fragmentView.findViewById(R.id.gridview);
		searchStatus = (TextView) fragmentView.findViewById(R.id.searchTextView);
		
		registerForContextMenu(mGridView);
		mGridView.setOnCreateContextMenuListener(this);

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				// Toast.makeText(SubscriptionsFragment.this.getActivity(), "" +
				// position, Toast.LENGTH_SHORT).show();
				Activity activity = SubscriptionsFragment.this.getActivity();
				if (activity instanceof DrawerActivity) {

					Cursor cursor = (Cursor) mGridView.getAdapter().getItem(
							position);

					Subscription sub = null;
					try {
						sub = Subscription.getByCursor(cursor);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}

					if (sub != null) {
						if (contentType.equals(ContentType.LOCAL))
							((MainActivity) activity).onItemSelected(sub.getId());
						else {
							sub.subscribe(getActivity());
							String text = "Subscribing to: " + sub.getTitle();
							Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
						}
					}
				}
			}

		});

		Intent intent = getActivity().getIntent();

		Uri uri = intent.getData();

		// startInit();
		return fragmentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		CursorAdapter ca = getSubscriptionCursorAdapter(getActivity(),
				mFragmentUtils.getCursor());
		mFragmentUtils.setAdapter(ca);
		// String condition = SubscriptionColumns.STATUS + "<>" +
		// Subscription.STATUS_UNSUBSCRIBED ;
		fetchLocalCursor();
		mGridView.setAdapter(ca);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.subscription_actionbar, menu);
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
		
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
	    	
	    	private QueryGpodder asyncTask = null;

	        @Override
	        public boolean onQueryTextSubmit(String query) {
	            // collapse the view ?
	            menu.findItem(R.id.menu_search).collapseActionView();
	            return false;
	        }

	        @Override
	        public boolean onQueryTextChange(String newText) {
	            // search goes here !!
	        	if (newText != null && !newText.equals("")) {
	        		mFragmentUtils.getAdapter().changeCursor(newMatrixCursor());
	        		if (asyncTask != null && asyncTask.getStatus() != AsyncTask.Status.FINISHED)
	        			asyncTask.cancel(true);
	        		
	        		asyncTask = new QueryGpodder();
	        		asyncTask.execute(newText);
	        	} else
	        		fetchLocalCursor();
	        	
	            return false;
	        }

	    });
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.subscription_context, menu);
		RecentItemFragment.setContextMenu(RecentItemFragment.SUBSCRIPTION_CONTEXT_MENU, this);
	}
	
	/**
	 * FIXME this should be moved somewhere else
	 * 
	 * @param menuItem
	 * @return
	 */
	public boolean subscriptionContextMenu(MenuItem menuItem) {

		if (!AdapterView.AdapterContextMenuInfo.class.isInstance(menuItem
				.getMenuInfo()))
			return false;

		AdapterView.AdapterContextMenuInfo cmi = (AdapterView.AdapterContextMenuInfo) menuItem
				.getMenuInfo();

		Cursor cursor = getAdapter().getCursor();
		int pos = cmi.position;
		cursor.moveToPosition(pos);
		Subscription subscription = Subscription.getByCursor(cursor);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuItem
				.getMenuInfo();
		switch (menuItem.getItemId()) {
		case R.id.unsubscribe:
			subscription.unsubscribe(getActivity());
			CursorAdapter adapter = getAdapter();
			if (adapter != null)
				getAdapter().notifyDataSetChanged();
			return true;
		default:
			return super.onContextItemSelected(menuItem);
		}
	}
	
	private void fetchLocalCursor() {
		String order = getOrder();
		String where = getWhere();
		mFragmentUtils.startInit(0, SubscriptionColumns.URI,
				SubscriptionColumns.ALL_COLUMNS, where, order);
		contentType = ContentType.LOCAL;
		setSearchStatusVisible(false);
	}

	public void startInit() {
		Cursor cursor = new CursorLoader(getActivity(),
				SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, null,
				null, null).loadInBackground();
		CursorAdapter cursorAdapter = getSubscriptionCursorAdapter(
				getActivity().getApplicationContext(), cursor);

		mFragmentUtils.setCursor(cursor);
		mFragmentUtils.setAdapter(cursorAdapter);

		mGridView.setAdapter(cursorAdapter);
	}

	private static SubscriptionListCursorAdapter listSubscriptionCursorAdapter(
			Context context, Cursor cursor) {
		SubscriptionListCursorAdapter.FieldHandler[] fields = {
				AbstractPodcastAdapter.defaultTextFieldHandler,
				new SubscriptionListCursorAdapter.IconFieldHandler() };
		return new SubscriptionListCursorAdapter(context,
				R.layout.subscription_list_item, cursor, new String[] {
						SubscriptionColumns.TITLE,
						SubscriptionColumns.IMAGE_URL }, new int[] {
						R.id.title, R.id.list_image }, fields);
	}

	private static SubscriptionGridCursorAdapter gridSubscriptionCursorAdapter(
			Context context, Cursor cursor) {
		SubscriptionGridCursorAdapter.FieldHandler[] fields = {
				AbstractPodcastAdapter.defaultTextFieldHandler,
				new SubscriptionGridCursorAdapter.IconFieldHandler() };
		return new SubscriptionGridCursorAdapter(context,
				R.layout.subscription_grid_item, cursor, new String[] {
						SubscriptionColumns.TITLE,
						SubscriptionColumns.IMAGE_URL }, new int[] {
						R.id.title, R.id.list_image }, fields);
	}

	public CursorAdapter getAdapter(Cursor cursor) {
		CursorAdapter adapter = mFragmentUtils.getAdapter();
		if (adapter != null)
			return adapter;

		adapter = getSubscriptionCursorAdapter(this.getActivity(), cursor);
		mFragmentUtils.setAdapter(adapter);
		return adapter;
	}

	public int getItemLayout() {
		// TODO Auto-generated method stub
		return 0;
	}

	String getWhere() {
		String whereClause = SubscriptionColumns.STATUS + "<>"
				+ Subscription.STATUS_UNSUBSCRIBED;
		whereClause = whereClause + " OR " + SubscriptionColumns.STATUS
				+ " IS NULL"; // Deprecated.
		return whereClause;
	}

	String getOrder() {
		return PodcastBaseFragment.orderByFirst(SubscriptionColumns.TITLE
				+ "<> ''")
				+ ", " + SubscriptionColumns.TITLE + " ASC";
	}

	private int getLayoutType() {
		return (displayLayout == LayoutType.GRID) ? R.layout.subscription_list
				: R.layout.channel_new;
	}

	public CursorAdapter getAdapter() {
		// FIXME deprecated
		Cursor cursor = new CursorLoader(getActivity(),
				SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, getWhere(),
				null, getOrder()).loadInBackground();
		return getSubscriptionCursorAdapter(getActivity(), cursor);
	}

	// Should this be public?
	public CursorAdapter getSubscriptionCursorAdapter(Context context,
			Cursor cursor) {
		if ((displayLayout == LayoutType.GRID))
			return gridSubscriptionCursorAdapter(context, cursor);
		else
			return listSubscriptionCursorAdapter(context, cursor);
	}
	
	private void setSearchStatusVisible(boolean isVisible) {
		if (isVisible) {
			searchStatus.setVisibility(View.VISIBLE);
		} else {
			searchStatus.setVisibility(View.GONE);
		}
	}

	private Cursor cursorFromSearchResults(List<IPodcast> podcasts) {

		MatrixCursor matrixCursor = newMatrixCursor();

		int idIdx = matrixCursor.getColumnIndex(SubscriptionColumns._ID);
		int titleIdx = matrixCursor.getColumnIndex(SubscriptionColumns.TITLE);
		int logoIdx = matrixCursor.getColumnIndex(SubscriptionColumns.IMAGE_URL);
		int urlIdx = matrixCursor.getColumnIndex(SubscriptionColumns.URL);
		
		int autoDownloadIdx = matrixCursor.getColumnIndex(SubscriptionColumns.AUTO_DOWNLOAD);
		int itemUpdatedIdx = matrixCursor.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED);
		int updatedIdx = matrixCursor.getColumnIndex(SubscriptionColumns.LAST_UPDATED);
		int failIdx = matrixCursor.getColumnIndex(SubscriptionColumns.FAIL_COUNT);
		
		int nItems = SubscriptionColumns.ALL_COLUMNS.length;
		for (IPodcast podcast : podcasts) {
			ArrayList<Object> values = new ArrayList<Object>((Collections.nCopies(nItems, null)));
			String url = podcast.getUrl();
			
			// Since we cace by ID we need a unique ID for remote podcasts as well.
			// We do this by calculating the long value of sha1(url)
			values.set(idIdx, StringtoLong(url));
			values.set(titleIdx, podcast.getTitle());
			values.set(logoIdx, podcast.getLogoUrl());
			values.set(urlIdx, podcast.getUrl());
			
			values.set(autoDownloadIdx, (long)-1);
			values.set(itemUpdatedIdx, (long)-1);
			values.set(updatedIdx, (long)-1);
			values.set(failIdx, (long)-1);
			//values.add(statusIdx, Subscription.STATUS_UNSUBSCRIBED);

			matrixCursor.addRow(values);
		}

		mFragmentUtils.setCursor(matrixCursor);
		mFragmentUtils.getAdapter().changeCursor(matrixCursor);
		contentType = ContentType.REMOTE;
		return matrixCursor;
	}
	
	private MatrixCursor newMatrixCursor() {
		return new MatrixCursor(SubscriptionColumns.ALL_COLUMNS);
	}
	
	private long StringtoLong(String s) {

		MessageDigest mDigest = null;
		try {
			mDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] hashValBytes = mDigest.digest(s.getBytes());

		long hashValLong = 0;

		//create a long value from the byte array
		for( int i = 0; i < 8; i++ ) {
		    hashValLong |= ((long)(hashValBytes[i]) & 0x0FF)<<(8*i);
		}
		
		return hashValLong;
	}
	
	private class QueryGpodder extends AsyncTask<String, Void, List<IPodcast>> {
		
		public QueryGpodder() {
			setSearchStatusVisible(true);
		}
		
		protected List<IPodcast> doInBackground(String... string) {
			PublicClient gpodderClient = new PublicClient();
			List<IPodcast> podcasts = null;
			try {
				podcasts = gpodderClient.searchPodcast(string[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return podcasts;
		}

		protected void onPostExecute(List<IPodcast> podcasts) {
			SubscriptionsFragment.this.cursorFromSearchResults(podcasts);
		}
	}

}
