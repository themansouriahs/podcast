package org.bottiger.podcast;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bottiger.podcast.adapters.SubscriptionGridCursorAdapter;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.FragmentUtils;
import org.bottiger.podcast.views.dialogs.DialogOPML;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
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
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dragontek.mygpoclient.pub.PublicClient;
import com.dragontek.mygpoclient.simple.IPodcast;

public class SubscriptionsFragment extends Fragment implements SubscriptionGridCursorAdapter.OnPopulateSubscriptionList {

    public SubscriptionsFragment() {
        super();
    }

    @Override
    public void listPopulated(Cursor cursor) {
        setSubscriptionBackground(cursor);
    }

    private static enum LayoutType {
		LIST, GRID, FEED
	};
	
	private static enum ContentType {
		LOCAL, REMOTE, FEED, EMPTY
	};

    private Subscription mSubscription = null;
    public PodcastBaseFragment.OnItemSelectedListener mCLickListener = null;

	private FragmentUtils mFragmentUtils;
	private View fragmentView;
	private GridView mGridView;
	private TextView searchStatus;

    private ContentObserver mContentObserver;
    private Cursor mCursor;

    private RelativeLayout mEmptySubscrptionList;

    private Activity mActivity;
    private FrameLayout mContainerView;

	long id;
	
	private LayoutType mDisplayLayout = LayoutType.GRID;
	private ContentType mContentType = ContentType.LOCAL;

    private SharedPreferences shareprefs;
    private static final String PREF_SUBSCRIPTION_COLUMNS = "pref_subscriptions_columns";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mActivity = getActivity();
        shareprefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        mContainerView = (FrameLayout)inflater.inflate(R.layout.subscription_container, container, false);
        FrameLayout fragmentContainer = (FrameLayout)mContainerView.findViewById(R.id.subscription_fragment_container);

		fragmentView = inflater.inflate(getLayoutType(), fragmentContainer, true);

        mEmptySubscrptionList = (RelativeLayout) mContainerView.findViewById(R.id.subscription_empty);
        mEmptySubscrptionList.setVisibility(View.GONE);

		mFragmentUtils = new FragmentUtils(getActivity(), fragmentView, this);
		mGridView = (GridView) fragmentView.findViewById(R.id.gridview);
		searchStatus = (TextView) fragmentView.findViewById(R.id.searchTextView);

		registerForContextMenu(mGridView);

        mGridView.setNumColumns(numberOfColumns(mActivity));
		mGridView.setOnCreateContextMenuListener(this);

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

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
						if (mContentType.equals(ContentType.LOCAL))

                            if (mCLickListener == null) {
                                //((MainActivity) activity).onItemSelected(sub.getId());
                                FeedActivity.start(getActivity(), sub.getId());
                            } else {
                                mCLickListener.onItemSelected(sub.getId());
                            }
						else {
							sub.subscribe(getActivity());
							String text = mActivity.getString(R.string.subscription_subscribing_to) + sub.getTitle();
							Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
						}
					}
				}
			}

		});

		return mContainerView;
	}

    @Override
    public void onResume() {
        super.onResume();
        if (mGridView.getNumColumns() != numberOfColumns(mActivity)) {
            mGridView.setNumColumns(numberOfColumns(mActivity));
            mGridView.invalidate();
        }
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        mCursor = mFragmentUtils.getCursor();

        setSubscriptionBackground(mCursor);
		CursorAdapter ca = getSubscriptionCursorAdapter(getActivity(), mCursor);
		mFragmentUtils.setAdapter(ca);
		fetchLocalCursor();
		mGridView.setAdapter(ca);
	}

    private void setSubscriptionBackground(Cursor argCursor) {
        if (argCursor == null || argCursor.getCount() == 0) {
            mEmptySubscrptionList.setVisibility(View.VISIBLE);
            return;
        }

        mEmptySubscrptionList.setVisibility(View.GONE);
    }

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.subscription_actionbar, menu);


        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            private QueryGpodder asyncTask = null;

            @Override
            public boolean onQueryTextSubmit(String query) {
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
        return;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_import: {
                DialogOPML dialogOPML = new DialogOPML();
                Dialog dialog = dialogOPML.onCreateDialog(getActivity());
                dialog.show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.subscription_context, menu);
		PlaylistFragment.setContextMenu(PlaylistFragment.SUBSCRIPTION_CONTEXT_MENU, this);
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
		mContentType = ContentType.LOCAL;
		setSearchStatusVisible(false);
	}

	private static SubscriptionGridCursorAdapter gridSubscriptionCursorAdapter(
			Context context, SubscriptionGridCursorAdapter.OnPopulateSubscriptionList observer, Cursor cursor) {
		return new SubscriptionGridCursorAdapter(context,
				R.layout.subscription_grid_item, observer, cursor);
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
		return R.layout.subscription_list;
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
		if ((mDisplayLayout == LayoutType.GRID)) {
            setSubscriptionBackground(cursor);
            return gridSubscriptionCursorAdapter(context, this, cursor);
        } else
			return null; //listSubscriptionCursorAdapter(context, cursor);
	}
	
	private void setSearchStatusVisible(boolean isVisible) {
		if (isVisible) {
			searchStatus.setVisibility(View.VISIBLE);
		} else {
			searchStatus.setVisibility(View.GONE);
            setSubscriptionBackground(mCursor);
		}
	}

	private Cursor cursorFromSearchResults(List<IPodcast> podcasts) {

        MatrixCursor matrixCursor = newMatrixCursor();

        if (podcasts == null || podcasts.isEmpty())
            return matrixCursor;


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

        setSubscriptionBackground(matrixCursor);
		mFragmentUtils.setCursor(matrixCursor);
		mFragmentUtils.getAdapter().changeCursor(matrixCursor);
		mContentType = ContentType.REMOTE;
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


    private int numberOfColumns(@NonNull Activity argActivity) {
        String number = shareprefs.getString(PREF_SUBSCRIPTION_COLUMNS, "2");
        return Integer.parseInt(number);
    }

}
