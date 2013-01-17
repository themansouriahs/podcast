package info.bottiger.podcast;

import info.bottiger.podcast.R;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.utils.ControlButtons;
import info.bottiger.podcast.utils.DialogMenu;
import info.bottiger.podcast.utils.ExpandAnimation;
import info.bottiger.podcast.utils.FeedCursorAdapter;
import info.bottiger.podcast.utils.FeedCursorAdapter.TextFieldHandler;

import java.util.HashMap;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

public class RecentItemFragment extends PodcastBaseFragment {

	private static final int MENU_REFRESH = Menu.FIRST + 1;
	private static final int MENU_SORT = Menu.FIRST + 2;
	private static final int MENU_SELECT = Menu.FIRST + 3;

	private static final int MENU_ITEM_VIEW_CHANNEL = Menu.FIRST + 8;
	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;
	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 12;

	private static final String[] PROJECTION = new String[] {
			ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.SUB_TITLE, //
			ItemColumns.IMAGE_URL, //
			ItemColumns.DURATION, //
			ItemColumns.STATUS, // 
			ItemColumns.SUBS_ID, //
			ItemColumns.FILESIZE,
			ItemColumns.PATHNAME, //
			ItemColumns.OFFSET, //
			ItemColumns.KEEP //

	};

	private static HashMap<Integer, Integer> mIconMap;
	public static HashMap<Integer, Integer> mKeepIconMap;

	private View mCurrentPlayer = null;
	private ListView actualListView = null;
	
	
	private long pref_order;
	private long pref_where;
	private long pref_select;
	/*
	 * private long pref_select_bits; //bitmask of which status values to
	 * display private static long pref_select_bits_new = 1<<0; //new or viewed
	 * private static long pref_select_bits_download = 1<<1; //being downloaded
	 * private static long pref_select_bits_unplayed = 1<<2; //downloaded, not
	 * in playlist private static long pref_select_bits_inplay = 1<<3; //in
	 * playlist, play, pause private static long pref_select_bits_done = 1<<4;
	 * //done being played private static long pref_select_bits_all = -1; //all
	 * bits set
	 */

	private FeedCursorAdapter mAdapter;
	
    boolean mDualPane;
	private long mCurCheckID = -1;
	
	// Read here: http://developer.android.com/reference/android/app/Fragment.html#Layout
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckID = savedInstanceState.getLong("curChoice", 0);
        }

        // Populate list with our static array of titles.
        startInit();
        
        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        //View detailsFrame = getActivity().findViewById(R.id.details);
        //mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            // Make sure our UI is in the correct state.
            //showDetails(mCurCheckPosition);
        }
        
        final PullToRefreshListView pullToRefreshView = (PullToRefreshListView) fragmentView.findViewById(R.id.episode_list);
        
        OnRefreshListener<ListView> pullToRefreshListener = new OnRefreshListener<ListView>() {
            
        	@Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
        		SwipeActivity.mServiceBinder.start_update(pullToRefreshView);
            }
        };
        
        actualListView =pullToRefreshView.getRefreshableView();
        pullToRefreshView.getLoadingLayoutProxy().setRefreshingLabel("Refreshing feeds");
        pullToRefreshView.setOnRefreshListener(pullToRefreshListener);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("curChoice", mCurCheckID);
    }
    
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.episode_list, menu);
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_download_all: {
			Cursor cursor = createCursor(getWhere());
			while (cursor.moveToNext()) {
			    FeedItem feedItem = FeedItem.getByCursor(cursor);
			    if (!feedItem.isDownloaded())
			    	PodcastDownloadManager.addItemToQueue(feedItem);
			}
		    PodcastDownloadManager pdm = new PodcastDownloadManager();
		    pdm.do_download(true, this.getActivity());
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}
	

	public static FeedCursorAdapter listItemCursorAdapter(Context context,
			PodcastBaseFragment fragment, Cursor cursor) {
		FeedCursorAdapter.FieldHandler[] fields = {
				FeedCursorAdapter.defaultTextFieldHandler,
				new TextFieldHandler(), new TextFieldHandler(),
				new FeedCursorAdapter.IconFieldHandler(mIconMap),
		// new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new FeedCursorAdapter(context, fragment, R.layout.list_item, cursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.IMAGE_URL },
				new int[] { R.id.title, R.id.podcast, R.id.duration,
						R.id.list_image }, fields);
	}

	public static FeedCursorAdapter channelListItemCursorAdapter(
			Context context, Cursor cursor) {
		FeedCursorAdapter.FieldHandler[] fields = {
				FeedCursorAdapter.defaultTextFieldHandler,
				new FeedCursorAdapter.IconFieldHandler(mIconMap),
				new FeedCursorAdapter.IconFieldHandler(mKeepIconMap) };
		return new FeedCursorAdapter(context, R.layout.channel_list_item,
				cursor, new String[] { ItemColumns.TITLE, ItemColumns.STATUS,
						ItemColumns.KEEP }, new int[] { R.id.text1, R.id.icon,
						R.id.actionbar_compat }, fields);
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

		// super.onCreate(savedInstanceState);

		fragmentView = inflater.inflate(R.layout.recent, container, false);
		// setContentView(R.layout.main);
		// setTitle(getResources().getString(R.string.title_episodes));

		// getListView().setOnCreateContextMenuListener(this);
		Intent intent = getActivity().getIntent();
		intent.setData(ItemColumns.URI);

		
		
		// FIXME
		// mPrevIntent = new Intent(this, ChannelsActivity.class);
		// mNextIntent = new Intent(this, DownloadingActivity.class);

		getPref();

		// mServiceBinder.
		// mServiceBinder.start_update();

		// TabsHelper.setEpisodeTabClickListeners(this,
		// R.id.episode_bar_all_button);

		//startInit();
		return fragmentView;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		mCurCheckID = id;
		//ListView list = getListView();
		ListView list = actualListView;
		int start = list.getFirstVisiblePosition();
		for (int i = start, j = list.getLastVisiblePosition(); i <= j; i++) {
			Cursor item = (Cursor) list.getItemAtPosition(i+1); //FIXME https://github.com/chrisbanes/Android-PullToRefresh/issues/99

			if (id == item.getLong(item.getColumnIndex(BaseColumns._ID))) {
				this.togglePlayer(list, item);
			}

		}

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
		dialog_menu.addMenu(MENU_ITEM_VIEW_CHANNEL,
				getResources().getString(R.string.menu_view_channel));

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
			switch (mMenu.getSelect(select)) {
			case MENU_ITEM_DETAILS: {
				// FeedItem.view(AllItemFragment.this, item_id); FIXME
				return;
			}
			case MENU_ITEM_VIEW_CHANNEL: {
				// FeedItem.viewChannel(AllItemFragment.this, item_id); FIXME
				return;
			}

			case MENU_ITEM_START_DOWNLOAD: {

				FeedItem feeditem = FeedItem.getById(getActivity()
						.getContentResolver(), item_id);
				if (feeditem == null)
					return;

				feeditem.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
				feeditem.update(getActivity().getContentResolver());
				SwipeActivity.mServiceBinder.start_download();
				return;
			}
			case MENU_ITEM_START_PLAY: {
				// FeedItem.play(AllItemFragment.this, item_id); FIXME
				return;
			}
			case MENU_ITEM_ADD_TO_PLAYLIST: {
				// FeedItem.addToPlaylist(AllItemFragment.this, item_id); FIXME
				return;
			}
			}
		}
	}

	@Override
	public void startInit() {
		showEpisodes(getWhere());
		super.startInit();
	}

	public void showPlayingEpisode(long playingEpisodeID) {
		// this.getActivity().findViewById(id)
		//ViewStub stub = (ViewStub) getActivity().findViewById(R.id.stub_play);
		//View inflated = stub.inflate();

		FeedItem episode = FeedItem.getById(getActivity().getContentResolver(),
				playingEpisodeID);

		//TextView t = (TextView) inflated.findViewById(R.id.player_title); FIXME
		//t.setText(episode.title); FIXME
		listNonPlayingEpisodes(playingEpisodeID);
	}

	public void listNonPlayingEpisodes(long playingEpisodeID) {
		//String excludePLayingEpisode = ItemColumns._ID + "!="
		//		+ playingEpisodeID;
		String excludePLayingEpisode = "";
		showEpisodes(excludePLayingEpisode);
	}

	public void showEpisodes(String condition) {
		mCursor = createCursor(condition);

		mAdapter = RecentItemFragment.listItemCursorAdapter(this.getActivity(), this,
				mCursor);
		
		if (this.mCurCheckID > 0) {
			mAdapter.showItem(mCurCheckID);
			//View view = getViewByID(mCurCheckID);
			//this.setPlayerListeners(view, mCurCheckID);
		}
		
		setListAdapter(mAdapter);
	}

	public String getOrder() {
		String order = ItemColumns.DATE + " DESC LIMIT 20"; // before:
													// ItemColumns.CREATED
		if (pref_order == 0) {
			order = ItemColumns.SUBS_ID + "," + order;
		} else if (pref_order == 1) {
			order = ItemColumns.STATUS + "," + order;
		}
		return order;
	}

	public String getWhere() {
		String where = "";
		
		int foo = 56;
		if (foo == 56)
			return where;
		//where = where + ItemColumns.STATUS + "<"
		//		+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;
		switch ((int) pref_select) {
		case 1: // New only
			where = ItemColumns.STATUS + "<"
					+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW;
			break;
		case 2: // Unplayed only
			where = ItemColumns.STATUS + "=" + ItemColumns.ITEM_STATUS_NO_PLAY;
			break;
		case 3: // Playable only
			where = "(" + where + ") AND (" + ItemColumns.STATUS + ">"
					+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + ")";
			break;
		default: // case 0 = All, no change to initial where clause
			; // treat any unknown values as "All"
		}
		return where;
	}

	public void getPref() {
		SharedPreferences pref = getActivity().getSharedPreferences(
				SettingsActivity.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
		pref_order = pref.getLong("pref_order", 2);
		pref_where = pref.getLong("pref_where", 0);
		pref_select = pref.getLong("pref_select", 0);
	}
	
	private void togglePlayer(ListView list, Cursor item) {
		int start = list.getFirstVisiblePosition();
		boolean setListners = false;
		
		mAdapter.toggleItem(item);
		long id = item.getLong(item.getColumnIndex(BaseColumns._ID));
		String duration = item.getString(item.getColumnIndex(ItemColumns.DURATION));
		int position = item.getPosition();
		View view = list.getChildAt(position - start +1);
		mAdapter.notifyDataSetChanged();
		
		ControlButtons.Holder viewHolder = new ControlButtons.Holder();
		viewHolder.currentTime = (TextView) view.findViewById(R.id.current_position);
		viewHolder.duration = (TextView) view.findViewById(R.id.duration);
		if (viewHolder.duration != null) viewHolder.duration.setText(duration);
		
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
		
		//if (setListners) {
		//	setPlayerListeners(view, id);
		//}
		ControlButtons.setPlayerListeners(view, id);
		
		//updateCurrentPosition(FeedItem.getById(getActivity().getContentResolver(), id));
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
	
	private Cursor createCursor(String condition) {
		//return new CursorLoader(getActivity(), ItemColumns.URI, PROJECTION,
		//		condition, null, getOrder()).loadInBackground();
		return new CursorLoader(getActivity(), ItemColumns.URI, ItemColumns.ALL_COLUMNS,
				condition, null, getOrder()).loadInBackground();
	}
	
	@Override
	Subscription getSubscription(Object o) {
		Cursor item = (Cursor)o;
		Long id = item.getLong(item.getColumnIndex(ItemColumns.SUBS_ID));
		new Subscription();
		return Subscription.getById(getActivity().getContentResolver(), id);
	}
}
