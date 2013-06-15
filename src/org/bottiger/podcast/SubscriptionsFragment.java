package org.bottiger.podcast;

import java.util.HashMap;

import org.bottiger.podcast.R;
import org.bottiger.podcast.R.id;
import org.bottiger.podcast.R.layout;
import org.bottiger.podcast.R.string;
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class SubscriptionsFragment extends Fragment {

	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;
	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 12;

	private static enum LayoutType {
		LIST, GRID
	};

	private FragmentUtils mFragmentUtils;

	TextView addSubscriptionView = null;;
	private static HashMap<Integer, Integer> mIconMap;
	private View fragmentView;

	private GridView mGridView;

	Subscription mChannel = null;
	long id;
	private LayoutType displayLayout = LayoutType.GRID;

	static {
		mIconMap = new HashMap<Integer, Integer>();
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

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				// Toast.makeText(SubscriptionsFragment.this.getActivity(), "" +
				// position, Toast.LENGTH_SHORT).show();
				Activity activity = SubscriptionsFragment.this.getActivity();
				if (activity instanceof MainActivity) {
					Cursor cursor = (Cursor) SubscriptionsFragment.this
							.getAdapter().getItem(position);

					Subscription sub = null;
					try {
						sub = Subscription.getByCursor(cursor);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}

					if (sub != null) {
						((MainActivity) activity).onItemSelected(sub.getId());
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
		String condition = "1==1";
		mFragmentUtils.startInit(0, SubscriptionColumns.URI,
				SubscriptionColumns.ALL_COLUMNS, getWhere(), getOrder());
		mGridView.setAdapter(ca);
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

	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getActivity()
				.getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}

		DialogMenu dialog_menu = new DialogMenu();

		dialog_menu.setHeader(feed_item.title);

		dialog_menu.addMenu(MENU_ITEM_DETAILS,
				getResources().getString(R.string.menu_details));

		if (feed_item.status < ItemColumns.ITEM_STATUS_MAX_READING_VIEW) {
			dialog_menu.addMenu(MENU_ITEM_START_DOWNLOAD, getResources()
					.getString(R.string.menu_download));
		} else if (feed_item.status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW) {
			dialog_menu.addMenu(MENU_ITEM_START_PLAY,
					getResources().getString(R.string.menu_play));
			dialog_menu.addMenu(MENU_ITEM_ADD_TO_PLAYLIST, getResources()
					.getString(R.string.menu_add_to_playlist));
		}

		return dialog_menu;
	}

	class MainClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;

		public MainClickListener(DialogMenu menu, long id) {
			mMenu = menu;
			item_id = id;
		}

		@Override
		public void onClick(DialogInterface dialog, int select) {
		}
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
		return SubscriptionColumns.TITLE + " ASC";
	}

	private int getLayoutType() {
		return (displayLayout == LayoutType.GRID) ? R.layout.subscription_list
				: R.layout.channel_new;
	}

	public CursorAdapter getAdapter() {
		Cursor cursor = new CursorLoader(getActivity(),
				SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, null,
				null, null).loadInBackground();
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

}
