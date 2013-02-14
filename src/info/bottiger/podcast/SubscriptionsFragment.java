package info.bottiger.podcast;

import info.bottiger.podcast.adapters.AbstractPodcastAdapter;
import info.bottiger.podcast.adapters.SubscriptionListCursorAdapter;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.provider.SubscriptionColumns;
import info.bottiger.podcast.utils.DialogMenu;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/* Copy of ChannelActivity */
public class SubscriptionsFragment extends PodcastBaseFragment {

	private static final int MENU_UNSUBSCRIBE = Menu.FIRST + 1;
	private static final int MENU_AUTO_DOWNLOAD = Menu.FIRST + 2;

	
	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;
	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 12;
	
	private static final String[] PROJECTION = new String[] {
		SubscriptionColumns._ID, // 0
		SubscriptionColumns.TITLE, // 1
		SubscriptionColumns.LINK,
		SubscriptionColumns.IMAGE_URL,
		SubscriptionColumns.RATING,
		SubscriptionColumns.LAST_UPDATED,
		SubscriptionColumns.COMMENT
	};

	TextView addSubscriptionView = null;;
	private static HashMap<Integer, Integer> mIconMap;
	private View fragmentView;
	
	Subscription mChannel = null;
	long id;

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
		
		return (ch!=null);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		fragmentView = inflater.inflate(R.layout.channel, container, false);
		
		Intent intent = getActivity().getIntent();

		Uri uri = intent.getData();
		
		mPrevIntent = null;
		mNextIntent = null;
		startInit();
		return fragmentView;
	}
	
    @Override 
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

		mAdapter = listSubscriptionCursorAdapter(getActivity(), mCursor);
		startInit(0, SubscriptionColumns.URI, PROJECTION, "", "");
    }

	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Subscription s = Subscription.getById(getActivity().getContentResolver(), id);
        this.mListener.onItemSelected(s.id);
	}
	
	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getActivity().getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		
		dialog_menu.addMenu(MENU_ITEM_DETAILS, 
				getResources().getString(R.string.menu_details));
		
		if(feed_item.status<ItemColumns.ITEM_STATUS_MAX_READING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_DOWNLOAD, 
					getResources().getString(R.string.menu_download));			
		}else if(feed_item.status>ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_PLAY, 
					getResources().getString(R.string.menu_play));
			dialog_menu.addMenu(MENU_ITEM_ADD_TO_PLAYLIST, 
					getResources().getString(R.string.menu_add_to_playlist));
		}

		return dialog_menu;
	}	

	


	class MainClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;
		public MainClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}
		
        @Override
		public void onClick(DialogInterface dialog, int select) 
        {
		} 
	}

	
	private static SubscriptionListCursorAdapter listSubscriptionCursorAdapter(Context context, Cursor cursor) {
		SubscriptionListCursorAdapter.FieldHandler[] fields = {
				AbstractPodcastAdapter.defaultTextFieldHandler,
				new SubscriptionListCursorAdapter.IconFieldHandler()
		};
		return new SubscriptionListCursorAdapter(context, R.layout.episode_list, cursor,
				new String[] { SubscriptionColumns.TITLE, SubscriptionColumns.IMAGE_URL },
				new int[] { R.id.title, R.id.list_image },
				fields);
	}
	
	public SimpleCursorAdapter getAdapter(Cursor cursor) {
		return listSubscriptionCursorAdapter(this.getActivity(), cursor);
	}

	@Override
	public void startInit() {
		mCursor = new CursorLoader(getActivity(), SubscriptionColumns.URI, PROJECTION, null, null, null).loadInBackground();
		mAdapter = listSubscriptionCursorAdapter(getActivity().getApplicationContext(), mCursor);
	
		setListAdapter(mAdapter);

	}

	@Override
	Subscription getSubscription(Object o) {
		Cursor item = (Cursor)o;
		Long id = item.getLong(item.getColumnIndex(BaseColumns._ID));
		new Subscription();
		return Subscription.getById(getActivity().getContentResolver(), id);
	}
	
}
