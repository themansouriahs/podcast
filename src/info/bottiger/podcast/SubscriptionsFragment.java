package info.bottiger.podcast;

import info.bottiger.podcast.R;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.provider.SubscriptionColumns;
import info.bottiger.podcast.utils.DialogMenu;
import info.bottiger.podcast.utils.GoogleReader;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
		AllItemActivity.initFullIconMap(mIconMap);
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
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_UNSUBSCRIBE, 0,
				getResources().getString(R.string.unsubscribe)).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		

		
		menu.add(0, MENU_AUTO_DOWNLOAD, 0,"Auto Download").setIcon(
				android.R.drawable.ic_menu_set_as);
		
	
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(MENU_AUTO_DOWNLOAD);
		String auto;
		if(mChannel.auto_download==0){
			auto = getResources().getString(R.string.menu_auto_download);
		}else{
			auto = getResources().getString(R.string.menu_manual_download);
		}        
        item.setTitle(auto);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_UNSUBSCRIBE:
		
		new AlertDialog.Builder(ChannelFragment.this)
                .setTitle(R.string.unsubscribe_channel)
                .setPositiveButton(R.string.menu_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
							mChannel.delete(getContentResolver());	
							finish();
							dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.menu_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
			return true;
		case MENU_AUTO_DOWNLOAD:
			mChannel.auto_download = 1-mChannel.auto_download;
			mChannel.update(getContentResolver());	
			return true;			

		}
		return super.onOptionsItemSelected(item);
	}
	 */
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
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_DETAILS: {
    			Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
    			FeedItem item = FeedItem.getById(getActivity().getContentResolver(), item_id);
    			if ((item != null)
    					&& (item.status == ItemColumns.ITEM_STATUS_UNREAD)) {
    				item.status = ItemColumns.ITEM_STATUS_READ;
    				item.update(getActivity().getContentResolver());
    			}    			
    			startActivity(new Intent(Intent.ACTION_EDIT, uri));   
    			return;
    		}    		
			case MENU_ITEM_START_DOWNLOAD: {
	
				FeedItem feeditem = FeedItem.getById(getActivity().getContentResolver(), item_id);
				if (feeditem == null)
					return;
	
				feeditem.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
				feeditem.update(getActivity().getContentResolver());
				SwipeActivity.mServiceBinder.start_download();
				return;
			}
			case MENU_ITEM_START_PLAY: {
	
				FeedItem feeditem = FeedItem.getById(getActivity().getContentResolver(), item_id);
				if (feeditem == null)
					return;
		
				//feeditem.play(ChannelFragment.this); TODO
				return;
			}
			case MENU_ITEM_ADD_TO_PLAYLIST: {
				
				FeedItem feeditem = FeedItem.getById(getActivity().getContentResolver(), item_id);
				if (feeditem == null)
					return;
		
				feeditem.addtoPlaylist(getActivity().getContentResolver());
				return;
			}
    		}
		} 
	}

	@Override
	public void startInit() {
		SwipeActivity.mCursor = new CursorLoader(getActivity(), SubscriptionColumns.URI, PROJECTION, null, null, null).loadInBackground();
		mAdapter = AllItemActivity.channelListSubscriptionCursorAdapter(getActivity().getApplicationContext(), SwipeActivity.mCursor);
	
		setListAdapter(mAdapter);

		super.startInit();

	}

	Subscription getSubscription(Object o) {
		Cursor item = (Cursor)o;
		Long id = item.getLong(item.getColumnIndex(ItemColumns._ID));
		return new Subscription().getById(getActivity().getContentResolver(), id);
	}
	
}
