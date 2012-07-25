package info.bottiger.podcast;

import info.bottiger.podcast.R;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.utils.ControlButtons;
import info.bottiger.podcast.utils.DialogMenu;
import info.bottiger.podcast.utils.ExpandAnimation;
import info.bottiger.podcast.utils.FeedCursorAdapter;
import info.bottiger.podcast.utils.FeedCursorAdapter.TextFieldHandler;

import java.util.HashMap;

import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
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

	private View V;
	private FeedCursorAdapter mAdapter;
	private Cursor mCursor;

	static {

		mIconMap = new HashMap<Integer, Integer>();
		initFullIconMap(mIconMap);

		mKeepIconMap = new HashMap<Integer, Integer>();
		mKeepIconMap.put(1, R.drawable.keep);
		mKeepIconMap.put(FeedCursorAdapter.ICON_DEFAULT_ID, R.drawable.blank); // anything
																				// other
																				// than
																				// KEEP

	}

	public static void initFullIconMap(HashMap<Integer, Integer> iconMap) {
		iconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.feed_new);
		iconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.feed_viewed);

		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE,
				R.drawable.download_pause);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE,
				R.drawable.download_wait);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOADING_NOW,
				R.drawable.downloading);

		iconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.playable);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAY_READY, R.drawable.play_ready);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYING_NOW, R.drawable.playing);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAY_PAUSE, R.drawable.play_pause);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.played);
		// iconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.keep);
		// we now show KEEP status with a separate icon, based on separate DB
		// flag

		iconMap.put(FeedCursorAdapter.ICON_DEFAULT_ID,
				R.drawable.status_unknown); // default for unknowns
	}

	public static FeedCursorAdapter listItemCursorAdapter(Context context,
			Cursor cursor) {
		FeedCursorAdapter.FieldHandler[] fields = {
				FeedCursorAdapter.defaultTextFieldHandler,
				new TextFieldHandler(), new TextFieldHandler(),
				new FeedCursorAdapter.IconFieldHandler(mIconMap),
		// new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new FeedCursorAdapter(context, R.layout.list_item, cursor,
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
						R.id.keep_icon }, fields);
	}

	public static int mapToIcon(int status) {
		Integer iconI = mIconMap.get(status);
		if (iconI == null)
			iconI = mIconMap.get(FeedCursorAdapter.ICON_DEFAULT_ID); // look for
																		// default
																		// value
																		// in
																		// map
		int icon = (iconI != null) ? iconI.intValue()
				: R.drawable.status_unknown; // Use this icon when not in map
												// and no map default.
		// This allows going back to a previous version after data has been
		// added in a new version with additional status codes.
		return icon;
	}

	public void onResume() {
		super.onResume();
		if (mPlayerServiceBinder != null && mPlayerServiceBinder.isPlaying()) {
			long current_id = mPlayerServiceBinder.getCurrentItem().id;
			showPlayingEpisode(current_id);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// super.onCreate(savedInstanceState);

		V = inflater.inflate(R.layout.recent, container, false);
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

		startInit();
		return V;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		/*
		if (!mPlayerServiceBinder.isPlaying()) {
			mPlayerServiceBinder.play(id); 
			mPlayerServiceBinder.start(); 
		}
		*/
		
		ListView list = getListView();
		int start = list.getFirstVisiblePosition();
		for (int i = start, j = list.getLastVisiblePosition(); i <= j; i++) {
			Cursor item = (Cursor) list.getItemAtPosition(i);

			if (id == item.getLong(item.getColumnIndex(ItemColumns._ID))) {
				mAdapter.toggleItem(item);
				View view = list.getChildAt(i - start);
				mAdapter.notifyDataSetChanged();
				
				ControlButtons.Holder viewHolder = new ControlButtons.Holder();
				viewHolder.currentTime = (TextView) view.findViewById(R.id.current_position);
				
				ViewStub stub = (ViewStub) view.findViewById(R.id.stub);
				if (stub != null) {
					stub.inflate();
					ExpandAnimation expandAni = new ExpandAnimation(stub, 5000);
					stub.startAnimation(expandAni);
					
					viewHolder.playPauseButton = (ImageButton) view.findViewById(R.id.play_toggle);
					viewHolder.stopButton = (ImageButton) view.findViewById(R.id.stop);
					viewHolder.infoButton = (ImageButton) view.findViewById(R.id.info);
					viewHolder.downloadButton = (ImageButton) view.findViewById(R.id.download);
					viewHolder.queueButton = (ImageButton) view.findViewById(R.id.queue);
					viewHolder.currentTime = (TextView) view.findViewById(R.id.current_position);
					viewHolder.seekbar = (SeekBar) view.findViewById(R.id.progress);
					
					ControlButtons.setListener(this, viewHolder, id);
				} else { 
					View player = view.findViewById(R.id.stub_player);
					if (player.getVisibility() == View.VISIBLE) {
						player.setVisibility(View.GONE);
					} else {
						player.setVisibility(View.VISIBLE);
					}
				}
				
				updateCurrentPosition();
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

	public void startInit() {
		showEpisodes(getWhere());
		super.startInit();
	}

	public void showPlayingEpisode(long playingEpisodeID) {
		// this.getActivity().findViewById(id)
		ViewStub stub = (ViewStub) getActivity().findViewById(R.id.stub_play);
		View inflated = stub.inflate();

		FeedItem episode = FeedItem.getById(getActivity().getContentResolver(),
				playingEpisodeID);

		TextView t = (TextView) inflated.findViewById(R.id.player_title);
		t.setText(episode.title);
		listNonPlayingEpisodes(playingEpisodeID);
	}

	public void listNonPlayingEpisodes(long playingEpisodeID) {
		String excludePLayingEpisode = ItemColumns._ID + "!="
				+ playingEpisodeID;
		showEpisodes(excludePLayingEpisode);
	}

	public void showEpisodes(String condition) {
		mCursor = new CursorLoader(getActivity(), ItemColumns.URI, PROJECTION,
				condition, null, getOrder()).loadInBackground();

		mAdapter = RecentItemFragment.listItemCursorAdapter(this.getActivity(),
				mCursor);
		setListAdapter(mAdapter);
	}

	public String getOrder() {
		String order = ItemColumns.DATE + " DESC"; // before:
													// ItemColumns.CREATED
		if (pref_order == 0) {
			order = ItemColumns.SUBS_ID + "," + order;
		} else if (pref_order == 1) {
			order = ItemColumns.STATUS + "," + order;
		}
		return order;
	}

	public String getWhere() {
		String where = ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;
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
				Pref.HAPI_PREFS_FILE_NAME, Service.MODE_PRIVATE);
		pref_order = pref.getLong("pref_order", 2);
		pref_where = pref.getLong("pref_where", 0);
		pref_select = pref.getLong("pref_select", 0);
	}
}
