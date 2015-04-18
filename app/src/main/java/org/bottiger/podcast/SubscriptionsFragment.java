package org.bottiger.podcast;

import org.bottiger.podcast.adapters.SubscriptionGridCursorAdapter;
import org.bottiger.podcast.playlist.SubscriptionCursorLoader;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.utils.FragmentUtils;
import org.bottiger.podcast.views.dialogs.DialogOPML;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class SubscriptionsFragment extends Fragment {

	private FragmentUtils mFragmentUtils;
	private View fragmentView;
	private RecyclerView mGridView;

    private GridLayoutManager mGridLayoutmanager;
    private RelativeLayout mEmptySubscrptionList;
    private Cursor mCursor;

    private Activity mActivity;
    private FrameLayout mContainerView;

    private SharedPreferences shareprefs;
    private static String PREF_SUBSCRIPTION_COLUMNS;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mActivity = getActivity();
        PREF_SUBSCRIPTION_COLUMNS = mActivity.getResources().getString(R.string.pref_subscriptions_columns_key);
        shareprefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
	}

    SubscriptionCursorLoader mCursorLoader;

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
		mGridView = (RecyclerView) fragmentView.findViewById(R.id.gridview);

		registerForContextMenu(mGridView);

        /*
        Cursor cursor = new CursorLoader(getActivity(),
                SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, getWhere(),
                null, getOrder()).loadInBackground();
                */

        SubscriptionGridCursorAdapter adapter = new SubscriptionGridCursorAdapter(getActivity(), mCursor, getGridItemLayout());

        mGridLayoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(mGridLayoutmanager);
        mGridView.setAdapter(adapter);

        mCursorLoader = new SubscriptionCursorLoader(this, adapter, mCursor);

        /*
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
		*/

		return mContainerView;
	}

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGridLayoutmanager.getSpanCount() != numberOfColumns()) {
            GridLayoutManager layoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
            /*mGridView.setNumColumns(numberOfColumns(mActivity));
            loadCursor();
            mGridView.invalidate();*/
            mGridView.setLayoutManager(layoutmanager);
        }
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        //loadCursor();
	}

/*
    private void setSubscriptionBackground(Cursor argCursor) {
        if (argCursor == null || argCursor.getCount() == 0) {
            mEmptySubscrptionList.setVisibility(View.VISIBLE);
            return;
        }

        mEmptySubscrptionList.setVisibility(View.GONE);
    }*/

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.subscription_actionbar, menu);
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
            case R.id.menu_refresh_all_subscriptions: {
                SoundWaves.sSubscriptionRefreshManager.refreshAll();
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
	 * @return
	 */
    /*
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
	}*/

	private int getLayoutType() {
		return R.layout.subscription_list;
	}

    private int numberOfColumns() {
        String number = shareprefs.getString(PREF_SUBSCRIPTION_COLUMNS, "2");
        return Integer.parseInt(number);
    }

    private int getGridItemLayout() {
        int columnsCOunt = numberOfColumns();
        return columnsCOunt == 1 ? R.layout.subscription_list_item : R.layout.subscription_grid_item ;
    }

}
