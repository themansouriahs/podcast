package info.bottiger.podcast;

import java.text.DecimalFormat;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import info.bottiger.podcast.R;
import info.bottiger.podcast.adapters.ItemCursorAdapter;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.provider.SubscriptionColumns;
import info.bottiger.podcast.service.PlayerService;
import info.bottiger.podcast.utils.DialogMenu;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.PodcastProgressBar;
import info.bottiger.podcast.utils.StrUtils;

public class PlayerActivity extends ListActivity {
	protected static PlayerService mServiceBinder = null;
	protected final Log log = Log.getLog(getClass());
	protected static ComponentName mService = null;

	private static final int MENU_OPEN_AUDIO = Menu.FIRST + 1;
	private static final int MENU_REPEAT = Menu.FIRST + 2;
	private static final int MENU_LOAD_ALL = Menu.FIRST + 3;
	private static final int MENU_LOAD_BY_CHANNEL = Menu.FIRST + 4;
	private static final int MENU_REMOVE_ALL = Menu.FIRST + 5;

	private static final int MENU_PLAY = Menu.FIRST + 6;
	private static final int MENU_DETAILS = Menu.FIRST + 7;
	private static final int MENU_REMOVE = Menu.FIRST + 8;

	private static final int MENU_MOVE_UP = Menu.FIRST + 9;
	private static final int MENU_MOVE_DOWN = Menu.FIRST + 10;

	private static final int STATE_MAIN = 0;
	private static final int STATE_VIEW = 1;

	private long rwnd_interval = 7 * 1000;
	private long ffwd_interval = 30 * 1000;

	private boolean mShow = false;
	private long mID;
	private long pref_repeat;
	private long pref_rwnd_interval;
	private long pref_fas_fwd_interval;
	private String mTitle = "Player";
	// private FeedItem mCurrentItem;

	protected SimpleCursorAdapter mAdapter;
	protected Cursor mCursor = null;
	private static HashMap<Integer, Integer> mIconMap;

	private ImageButton mRwndButton;
	private ImageButton mFfwdButton;

	private ImageButton mPauseButton;
	private ImageButton mPrevButton;
	private ImageButton mNextButton;

	private TextView mCurrentTime;
	private TextView mTotalTime;
	private PodcastProgressBar mProgress;

	private static final String[] PROJECTION = new String[] {
			ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.STATUS,
			ItemColumns.LISTENED };

	static {
		mIconMap = new HashMap<Integer, Integer>();

	}

	public static HashMap<Integer, Integer> mKeepIconMap;

	protected static ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceBinder = ((PlayerService.PlayerBinder) service)
					.getService();
			// log.debug("onServiceConnected");
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mServiceBinder = null;
			// log.debug("onServiceDisconnected");
		}
	};

	private static final int REFRESH = 1;
	private static final int PLAYITEM = 2;

	private void queueNextRefresh(long delay) {
		Message msg = mHandler.obtainMessage(REFRESH);
		mHandler.removeMessages(REFRESH);
		if (mShow)
			mHandler.sendMessageDelayed(msg, delay);
	}

	private void play(FeedItem item) {
		if (item == null)
			return;
		if (mServiceBinder != null)
			mServiceBinder.play(item.id);
		updateInfo();
	}

	/**
	 * Update the player UI
	 * 
	 * @return
	 */
	private long refreshNow() {
		if (mServiceBinder == null)
			return 500;
		if (mID >= 0) {
			startPlay();
		}
		if (mServiceBinder.getUpdateStatus()) {
			updateInfo();
			mServiceBinder.setUpdateStatus(false);
		}

		try {
			if (mServiceBinder.isInitialized() == false) {
				mCurrentTime.setVisibility(View.INVISIBLE);
				mTotalTime.setVisibility(View.INVISIBLE);
				return 500;
			}
			long pos = mServiceBinder.position();
			long duration = mServiceBinder.duration();

			if (mServiceBinder.isPlaying() == false) {
				mCurrentTime.setVisibility(View.VISIBLE);
				mCurrentTime.setText(StrUtils.formatTime(pos));

				return 500;
			}

			long remaining = 1000 - (pos % 1000);

			if ((pos >= 0) && (duration > 0)) {
				mCurrentTime.setText(StrUtils.formatTime(pos));

				if (mServiceBinder.isInitialized()) {
					mCurrentTime.setVisibility(View.VISIBLE);
				}

				// mProgress.setProgress((int) (1000 * pos /
				// mServiceBinder.duration()));
			}

			setProgressBar(mProgress, mServiceBinder);

			return remaining;
		} catch (Exception ex) {
		}
		return 500;
	}

	static void setProgressBar(SeekBar progressBar, PlayerService playerService) {
		FeedItem item = playerService.getCurrentItem();
		long duration = playerService.duration();
		long position = playerService != null ? playerService.position() : 0;
		long secondary;

		// FIXME - just added this check to avoid a crash
		if (item != null) {
			if (item.isDownloaded())
				secondary = item.getCurrentFileSize();
			else
				secondary = (playerService.bufferProgress() * duration) / 100;
			setProgressBar(progressBar, duration, position, secondary);
		}
	}

	public static void setProgressBar(SeekBar progressBar, FeedItem item) {
		if (item.getCurrentFileSize() == 0)
			return;
		long secondary = item.isDownloaded() ? item.getCurrentFileSize()
				: (item.chunkFilesize / item.filesize);
		long duration = item.getDuration();
		setProgressBar(progressBar, duration, item.offset, secondary);
	}

	/**
	 * duration, progress, secondary should all be in units of ms
	 */
	public static void setProgressBar(SeekBar progressBar, long duration,
			long progress, long secondary) {
		if (duration == 0)
			return;
		int progressMax = progressBar.getMax();
		int primaryProgress = (int) ((progressMax * progress) / duration);
		int secondaryProgress = (int) ((progressMax * secondary) / duration);
		progressBar.setProgress(primaryProgress);
		progressBar.setSecondaryProgress(secondaryProgress);
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				long next = refreshNow();
				queueNextRefresh(next);
				// log.debug("REFRESH: "+next);
				break;

			default:
				break;
			}
		}
	};

	// The update should happen elsewhere.
	// Not even sure this is really used.
	@Deprecated
	private void updateInfo() {
		FeedItem item;
		if (mServiceBinder == null) {
			mTotalTime.setVisibility(View.INVISIBLE);
			setTitle(mTitle);
			mPauseButton.setImageResource(android.R.drawable.ic_media_play);
			return;

		}

		if (mServiceBinder.isInitialized() == false) {
			mTotalTime.setVisibility(View.INVISIBLE);
			setTitle(mTitle);
			mPauseButton.setImageResource(android.R.drawable.ic_media_play);
			return;
		}

		item = mServiceBinder.getCurrentItem();
		if (item == null) {
			log.error("isInitialized but no item!!!");
			return;
		}

		mTotalTime.setVisibility(View.VISIBLE);
		mTotalTime.setText(StrUtils.formatTime(mServiceBinder.duration()));
		setTitle(item.title);

		if (mServiceBinder.isPlaying() == false) {
			mPauseButton.setImageResource(android.R.drawable.ic_media_play);
		} else {
			mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
		}
	}

	private void doPauseResume() {
		try {
			if (mServiceBinder != null) {
				if (mServiceBinder.isInitialized()) {
					if (mServiceBinder.isPlaying()) {
						mServiceBinder.pause();
					} else {
						mServiceBinder.start();
					}
				}
				refreshNow();
				updateInfo();
			}
		} catch (Exception ex) {
		}
	}

	private View.OnClickListener mNextListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				if (mServiceBinder != null && mServiceBinder.isInitialized()) {
					// mServiceBinder.next();

				}
			} catch (Exception ex) {
			}
			updateInfo();
		}
	};

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			doPauseResume();
		}
	};

	private View.OnClickListener mPrevListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				if (mServiceBinder != null && mServiceBinder.isInitialized()) {
					if (mServiceBinder.position() > 5000)
						mServiceBinder.seek(0);
					else {
						// mServiceBinder.prev();
					}
				}
			} catch (Exception ex) {
			}
			updateInfo();
		}
	};

	private View.OnClickListener mRwndListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				if (mServiceBinder != null && mServiceBinder.isInitialized()) {
					long pos = mServiceBinder.position();
					long newPos = pos - rwnd_interval;
					if (newPos < 0)
						newPos = 0;
					mServiceBinder.seek(newPos);

				}
			} catch (Exception ex) {
			}
			updateInfo();
		}
	};

	private View.OnClickListener mFfwdListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				if (mServiceBinder != null && mServiceBinder.isInitialized()) {
					long pos = mServiceBinder.position();
					mServiceBinder.seek(pos + ffwd_interval);

				}
			} catch (Exception ex) {
			}
			updateInfo();
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		startService(new Intent(this, PlayerService.class));
		// setContentView(R.layout.audio_player);
		setContentView(R.layout.recent);
		setTitle(getResources().getString(R.string.title_episodes));
		getListView().setOnCreateContextMenuListener(this);

		final Intent intent = getIntent();
		mID = intent.getLongExtra("item_id", -1);
		setIntent(new Intent());

		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(mPauseListener);

		mRwndButton = (ImageButton) findViewById(R.id.rwnd);
		mRwndButton.requestFocus();
		mRwndButton.setOnClickListener(mRwndListener);

		mFfwdButton = (ImageButton) findViewById(R.id.ffwd);
		mFfwdButton.requestFocus();
		mFfwdButton.setOnClickListener(mFfwdListener);

		mPrevButton = (ImageButton) findViewById(R.id.prev);
		mPrevButton.requestFocus();
		mPrevButton.setOnClickListener(mPrevListener);

		mNextButton = (ImageButton) findViewById(R.id.next);
		mNextButton.requestFocus();
		mNextButton.setOnClickListener(mNextListener);

		SeekBar mProgress = (SeekBar) findViewById(R.id.progress);

		// mProgress = new PodcastProgressBar(this, mServiceBinder,
		// mCurrentTime);

		mTotalTime = (TextView) findViewById(R.id.totaltime);
		mCurrentTime = (TextView) findViewById(R.id.currenttime);

		// TabsHelper.setEpisodeTabClickListeners(this,
		// R.id.episode_bar_play_button);

		startInit();

		// updateInfo();

	}

	protected static ItemCursorAdapter channelListItemCursorAdapter(
			Context context, Cursor cursor) {
		ItemCursorAdapter.FieldHandler[] fields = {
				ItemCursorAdapter.defaultTextFieldHandler,
				new ItemCursorAdapter.IconFieldHandler(mIconMap),
				new ItemCursorAdapter.IconFieldHandler(mKeepIconMap) };
		return new ItemCursorAdapter(context, R.layout.subscription_list,
				cursor, new String[] { ItemColumns.TITLE, ItemColumns.STATUS,
						ItemColumns.LISTENED }, new int[] { R.id.text1,
						R.id.icon, R.id.icon }, fields);
	}

	public void startInit() {

		mService = startService(new Intent(this, PlayerService.class));
		Intent bindIntent = new Intent(this, PlayerService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + " AND "
				+ ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW + " AND "
				+ ItemColumns.FAIL_COUNT + " > 100";

		String order = ItemColumns.FAIL_COUNT + " ASC";

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, order);

		//mAdapter = channelListItemCursorAdapter(this, mCursor);
		/*
		 * mAdapter = new IconCursorAdapter(this, R.layout.channel_list_item,
		 * mCursor, new String[] { ItemColumns.TITLE,ItemColumns.STATUS }, new
		 * int[] { R.id.text1}, mIconMap);
		 */
		setListAdapter(mAdapter);

	}

	@Override
	protected void onResume() {
		super.onResume();
		getPref();

		mShow = true;
		if (mID >= 0) {
			startPlay();
		}
		queueNextRefresh(1);
		updateInfo();

	}

	@Override
	protected void onPause() {
		super.onPause();
		mShow = false;

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		log.debug("onLowMemory()");
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(serviceConnection);
		} catch (Exception e) {
			e.printStackTrace();

		}

		// stopService(new Intent(this, service.getClass()));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_REPEAT:
			getPref();
			new AlertDialog.Builder(this)
					.setTitle("Chose Repeat Mode")
					.setSingleChoiceItems(R.array.repeat_select,
							(int) pref_repeat,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int select) {

									pref_repeat = select;
									SharedPreferences prefsPrivate = getSharedPreferences(
											SettingsActivity.HAPI_PREFS_FILE_NAME,
											Context.MODE_PRIVATE);
									Editor prefsPrivateEditor = prefsPrivate
											.edit();
									prefsPrivateEditor.putLong("pref_repeat",
											pref_repeat);
									prefsPrivateEditor.commit();
									dialog.dismiss();

								}
							}).show();
			return true;
		case MENU_LOAD_ALL:
			loadItem(null);
			return true;
		case MENU_REMOVE_ALL:
			removeAll();
			return true;
		case MENU_LOAD_BY_CHANNEL:
			loadChannel();

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}

		DialogMenu dialog_menu = new DialogMenu();

		dialog_menu.setHeader(feed_item.title);

		dialog_menu.addMenu(MENU_PLAY,
				getResources().getString(R.string.menu_play));

		dialog_menu.addMenu(MENU_MOVE_UP,
				getResources().getString(R.string.menu_move_up));

		dialog_menu.addMenu(MENU_MOVE_DOWN,
				getResources().getString(R.string.menu_move_down));

		dialog_menu.addMenu(MENU_DETAILS,
				getResources().getString(R.string.menu_details));

		dialog_menu.addMenu(MENU_REMOVE,
				getResources().getString(R.string.menu_remove));

		return dialog_menu;
	}

	@Deprecated
	class PlayClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;

		public PlayClickListener(DialogMenu menu, long id) {
			mMenu = menu;
			item_id = id;
		}

		@Override
		public void onClick(DialogInterface dialog, int select) {
			FeedItem feeditem = FeedItem.getById(getContentResolver(), item_id);
			if (feeditem == null)
				return;

		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, id);
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			setResult(RESULT_OK, new Intent().setData(uri));
		} else {
			DialogMenu dialog_menu = createDialogMenus(id);
			if (dialog_menu == null)
				return;

			new AlertDialog.Builder(this)
					.setTitle(dialog_menu.getHeader())
					.setItems(dialog_menu.getItems(),
							new PlayClickListener(dialog_menu, id)).show();
		}

	}

	static DecimalFormat mTimeDecimalFormat = new DecimalFormat("00");

	@Deprecated
	private void startPlay() {
		if (mServiceBinder != null) {
			FeedItem item = FeedItem.getById(getContentResolver(), mID);
			if (item != null) {
				play(item);
			}

			mID = -1;
		}
	}

	@Deprecated
	private void loadChannel() {
		String[] arr = new String[100];
		final long[] id_arr = new long[100];

		String where = null;
		Cursor cursor = managedQuery(SubscriptionColumns.URI,
				SubscriptionColumns.ALL_COLUMNS, where, null, null);
		int size = 0;
		if (cursor != null && cursor.moveToFirst()) {
			do {
				Subscription sub = Subscription.getByCursor(cursor);
				if (sub != null) {
					arr[size] = new String(sub.title);
					id_arr[size] = sub.id;
					size++;
					if (size >= 100)
						break;
				}
			} while (cursor.moveToNext());
		}
		String[] select_arr = new String[size];
		for (int i = 0; i < size; i++) {
			select_arr[i] = arr[i];
		}

		new AlertDialog.Builder(this)
				.setTitle("Select Channel")
				.setSingleChoiceItems(select_arr, 0,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int select) {
								loadItem(" AND " + ItemColumns.SUBS_ID + "="
										+ id_arr[select]);

								dialog.dismiss();
							}
						}).show();
	}

	@Deprecated
	private void loadItem(String channel_condition) {

		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + " AND "
				+ ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW + " AND "
				+ ItemColumns.FAIL_COUNT + " < 101 ";
		if (channel_condition != null)
			where += channel_condition;

		final String sel = where;

		new Thread() {
			@Override
			public void run() {
				Cursor cursor = null;
				try {

					String order = ItemColumns.FAIL_COUNT + " ASC";

					cursor = managedQuery(ItemColumns.URI,
							ItemColumns.ALL_COLUMNS, sel, null, order);
					long ord = Long.valueOf(System.currentTimeMillis());

					if ((cursor != null) && cursor.moveToFirst()) {
						do {
							FeedItem item = FeedItem.getByCursor(cursor);

						} while (cursor.moveToNext());
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (cursor != null)
						cursor.close();
				}

			}
		}.start();

	}

	@Deprecated
	private void removeAll() {
		if (mServiceBinder != null)
			mServiceBinder.stop();
		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + " AND "
				+ ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW + " AND "
				+ ItemColumns.FAIL_COUNT + " > 100 ";
		final String sel = where;

		new Thread() {
			@Override
			public void run() {
				Cursor cursor = null;
				try {

					cursor = managedQuery(ItemColumns.URI,
							ItemColumns.ALL_COLUMNS, sel, null, null);

					if ((cursor != null) && cursor.moveToFirst()) {
						do {
							FeedItem item = FeedItem.getByCursor(cursor);

						} while (cursor.moveToNext());
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (cursor != null)
						cursor.close();
				}

			}
		}.start();

	}

	@Deprecated
	private void getPref() {
		SharedPreferences pref = getSharedPreferences(
				SettingsActivity.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
		pref_repeat = pref.getLong("pref_repeat", 0);
		pref_fas_fwd_interval = Integer.parseInt(pref.getString(
				"pref_fast_forward_interval", "30"));
		ffwd_interval = pref_fas_fwd_interval * 1000;
		pref_rwnd_interval = Integer.parseInt(pref.getString(
				"pref_rewind_interval", "7"));
		rwnd_interval = pref_rwnd_interval * 1000;

	}

}
