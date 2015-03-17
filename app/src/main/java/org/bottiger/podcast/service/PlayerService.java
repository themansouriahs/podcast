package org.bottiger.podcast.service;

import org.bottiger.podcast.Player.LegacyRemoteController;
import org.bottiger.podcast.Player.MetaDataControllerWrapper;
import org.bottiger.podcast.Player.PlayerHandler;
import org.bottiger.podcast.Player.PlayerPhoneListener;
import org.bottiger.podcast.Player.PlayerStateManager;
import org.bottiger.podcast.Player.SoundWavesPlayer;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.utils.PodcastLog;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import javax.annotation.Nullable;

/**
 * The service which handles the audio extended_player. This is responsible for playing
 * including controlling the playback. Play, pause, stop etc.
 * 
 * @author Arvid Böttiger
 */
public class PlayerService extends Service implements
		AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private static PlayerHandler sPlayerHandler;

	/** Which action to perform when a track ends */
	public static enum NextTrack {
		NONE, NEW_TRACK, NEXT_IN_PLAYLIST
	}

	private static NextTrack nextTrack = NextTrack.NEXT_IN_PLAYLIST;
	
	private static Playlist sPlaylist = new Playlist();;

	public static final int PlayerService_STATUS = 1;

	private static final String WHERE = ItemColumns.STATUS + ">"
			+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + " AND "
			+ ItemColumns.STATUS + "<"
			+ ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW + " AND "
			+ ItemColumns.FAIL_COUNT + " > 100";

	private static final String ORDER = ItemColumns.FAIL_COUNT + " ASC";

	private final PodcastLog log = PodcastLog.getLog(getClass());

	private SoundWavesPlayer mPlayer = null;
    private MediaController mController;

    private MetaDataControllerWrapper mMetaDataControllerWrapper;
    private PlayerStateManager mPlayerStateManager;
    private LegacyRemoteController mLegacyRemoteController;

	private NotificationManager mNotificationManager;
    @Nullable private NotificationPlayer mNotificationPlayer;
    @Nullable private MediaSessionManager mMediaSessionManager;

	// AudioManager
	private AudioManager mAudioManager;
	private ComponentName mControllerComponentName;

	private FeedItem mItem = null;
	private boolean mUpdate = false;
    private boolean mResumePlayback = false;

    private final String LOCK_NAME = "SoundWavesWifiLock";
    WifiManager.WifiLock wifiLock;

	/**
	 * Phone state listener. Will pause the playback when the phone is ringing
	 * and continue it afterwards
	 */
	private PhoneStateListener mPhoneStateListener = new PlayerPhoneListener(this);

    @TargetApi(21)
    public PlayerStateManager getPlayerStateManager() {
        return mPlayerStateManager;
    }

	public void startAndFadeIn() {
        PlayerHandler.handler.sendEmptyMessageDelayed(PlayerHandler.FADEIN, 10);
	}

    public SoundWavesPlayer getPlayer() {
        return mPlayer;
    }

	@Override
	public void onCreate() {
		super.onCreate();

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, LOCK_NAME);

        sPlaylist.setContext(this);
		sPlaylist.populatePlaylistIfEmpty();

        sPlayerHandler = new PlayerHandler(this);
		
		mPlayer = new SoundWavesPlayer(this);
		mPlayer.setHandler(PlayerHandler.handler);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		this.mControllerComponentName = new ComponentName(this,
				HeadsetReceiver.class);
		log.debug("onCreate(): " + mControllerComponentName);
		this.mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // from https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediabrowserservice/MusicService.java
            // Start a new MediaSession
            mPlayerStateManager = new PlayerStateManager(this);
            mController = new MediaController(getApplicationContext(), mPlayerStateManager.getToken()); // .fromToken( mSession.getSessionToken() );

            mMetaDataControllerWrapper = new MetaDataControllerWrapper(mPlayerStateManager);
        } else {
            mLegacyRemoteController = new LegacyRemoteController();
            mMetaDataControllerWrapper = new MetaDataControllerWrapper(mLegacyRemoteController);
        }
	}

    @TargetApi(21)
    private void handleIntent( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;

        String action = intent.getAction();

        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
            //mController.getTransportControls().play();
            start();
        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            //mController.getTransportControls().pause();
            pause();
        } else if( action.equalsIgnoreCase( ACTION_FAST_FORWARD ) ) {
            mController.getTransportControls().fastForward();
        } else if( action.equalsIgnoreCase( ACTION_REWIND ) ) {
            mController.getTransportControls().rewind();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
            mController.getTransportControls().skipToPrevious();
        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
            mController.getTransportControls().skipToNext();
        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            mController.getTransportControls().stop();
        }
    }

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (isPlaying()) {
                // Pause playback
                pause();
                mResumePlayback = true;
            }
		} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// Resume playback
			if (mResumePlayback) {
				start();
				mResumePlayback = false;
			}
		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
			mAudioManager
					.unregisterMediaButtonEventReceiver(mControllerComponentName);
			mAudioManager.abandonAudioFocus(this);
			// Stop playback
			stop();
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		log.debug("onStart()");
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent( intent );

        mMetaDataControllerWrapper.register(this);

        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mPlayer != null) {
			mPlayer.release();
		}

		log.debug("onDestroy()");
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		log.debug("onLowMemory()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/**
	 * Hide the notification
	 */
    public void dis_notifyStatus() {
		// //mNotificationManager.cancel(R.layout.playing_episode);
		// setForeground(false);
		if (mNotificationPlayer != null)
			mNotificationPlayer.hide();
	}

	/**
	 * Display a notification with the current podcast
	 */
    public Notification notifyStatus() {

		if (mNotificationPlayer == null)
			mNotificationPlayer = new NotificationPlayer(this, mItem);

        mNotificationPlayer.setPlayerService(this);
		return mNotificationPlayer.show();
    }

	public void playNext(long nextId) {
		// assert playlistAdapter != null;

		// Cursor firstItem = (Cursor) playlistAdapter.getItem(0);
		// playlistAdapter.notifyDataSetChanged();
		if (mItem != null)
			mItem.trackEnded(getContentResolver());
		play(nextId);
	}

	public void play(long id) {

		// Pause the current episode in order to save the current state
		if (mPlayer.isPlaying())
			mPlayer.pause();

		if (mItem != null) {
			if ((mItem.id == id) && mPlayer.isInitialized()) {
				if (mPlayer.isPlaying() == false) {
					start();
				}
				return;
			}

			if (mPlayer.isPlaying()) {
				mItem.updateOffset(getContentResolver(), mPlayer.position());
				stop();
			}
		}

		mItem = FeedItem.getById(getContentResolver(), id);

		if (mItem == null)
			return;

		String dataSource = mItem.isDownloaded() ? mItem.getAbsolutePath()
				: mItem.getURL();

		int offset = mItem.offset < 0 ? 0 : mItem.offset;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()    );

        if (offset == 0 && prefs.getBoolean("pref_stream_proxy", false))
            dataSource = HTTPDService.proxyURL(mItem.id);

		mPlayer.setDataSourceAsync(dataSource, offset);
		
	    new Thread(new Runnable() {
	        public void run() {
	        	if (mItem.priority != 1)
	        		mItem.setPriority(null, getApplication());
	    		mItem.update(getContentResolver());
	        }
	    }).start();
	    
	}

    /**
     *
     * @param id
     * @return True of the songs start to play
     */
	public boolean toggle(long id) {
		if (mPlayer.isPlaying() == false || this.getCurrentItem().getId() != id) {
			play(id);
            return true;
		} else {
			mPlayer.pause();
            return false;
		}
	}

	@Deprecated
	public void toggle() {
		if (mPlayer.isPlaying() == false && mItem != null) {
			start();
		} else {
			pause();
		}
	}

	public void start() {
		if (mPlayer.isPlaying() == false) {
            takeWakelock(mPlayer.isSteaming());
			mPlayer.start();
            mMetaDataControllerWrapper.updateState(getCurrentItem(), true);
		}
	}

	public void pause() {
		if (mPlayer.isPlaying() == false) {
			return;
		}

		if ((mItem != null)) {
			mItem.updateOffset(getContentResolver(), mPlayer.position());
		} else {
			log.error("playing but no item!!!");

		}
		dis_notifyStatus();

		mPlayer.pause();
        mMetaDataControllerWrapper.updateState(mItem, false);
        releaseWakelock();
	}

	public void stop() {
		pause();
		mPlayer.stop();
		mItem = null;
		dis_notifyStatus();
		mUpdate = true;
	}

	public boolean isInitialized() {
		return mPlayer.isInitialized();
	}

	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	/**
	 * Test of the extended_player is on pause right now
	 * 
	 * @return True if the extended_player is on pause right now
	 */
	public boolean isOnPause() {
		if (isPlaying() || getCurrentItem() == null)
			return false;

		return true;
	}

	public long seek(long offset) {
		offset = offset < 0 ? 0 : offset;

		return mPlayer.seek(offset);

	}

	public long position() {
		return mPlayer.position();
	}

	public long duration() {
		return mPlayer.duration();
	}

	public int bufferProgress() {
		int test = mPlayer.getBufferProgress();
		return test;// mPlayer.bufferProgress;
	}

	public void setCurrentItem(FeedItem item) {
		stop();
		mItem = item;
	}

	public FeedItem getCurrentItem() {
		return mItem;
	}

	public boolean getUpdateStatus() {
		return mUpdate;
	}

	public void setUpdateStatus(boolean update) {
		mUpdate = update;
	}

	private FeedItem getFirst() {
		Cursor cursor = null;
		try {

			cursor = getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, WHERE, null, ORDER);
			if (cursor == null) {
				return null;
			}
			cursor.moveToFirst();

			FeedItem item = FeedItem.getByCursor(cursor);
			return item;
		} catch (Exception e) {

		} finally {
			if (cursor != null)
				cursor.close();
		}

		return null;
	}

	/**
	 * @return The ID of the next episode in the playlist
	 */
	public long getNextId() {
		FeedItem next = sPlaylist.nextEpisode();
		if (next != null)
			return next.getId();
		return -1;
	}

	/**
	 * Returns whether the next episode to be played should come from the
	 * playlist, or somewhere else
	 * 
	 * @return The type of episode to be played next
	 */
	public static NextTrack getNextTrack() {
		return nextTrack;
	}

    /**
     * Set wakelocks
     */
    public void takeWakelock(boolean isSteaming) {
        if (wifiLock.isHeld())
            return;

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mNotificationPlayer != null) {
            Notification notification = mNotificationPlayer.getNotification();
            if (notification != null) {
                startForeground(NotificationPlayer.getNotificationId(), notification);
            }
        }

        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        if (isSteaming && mWifi.isConnected()) {
            wifiLock.acquire();
        }
    }

    /**
     * Remove wakelocks
     */
    public void releaseWakelock() {
        stopForeground(true);
        //mPlayer.release();

        if (wifiLock.isHeld())
            wifiLock.release();
    }

	/**
	 * 
	 * @param nextTrack
	 */
	public static void setNextTrack(NextTrack nextTrack) {
		PlayerService.nextTrack = nextTrack;
	}

	public FeedItem getPrev(FeedItem item) {
		FeedItem prev_item = null;
		FeedItem curr_item = null;

		Cursor cursor = null;

		if (item == null) {
			FeedItem.getByCursor(cursor);
			return null;
		}

		try {
			cursor = getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, WHERE, null, ORDER);
			if (cursor == null) {
				return null;
			}
			cursor.moveToFirst();

			do {
				prev_item = curr_item;
				curr_item = FeedItem.getByCursor(cursor);

				if ((curr_item != null) && (item.id == curr_item.id)) {
					return prev_item;
				}

			} while (cursor.moveToNext());

		} catch (Exception e) {

		} finally {
			if (cursor != null)
				cursor.close();
		}

		return null;
	}

	public boolean getPref() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return prefs.getBoolean("pref_continuously_playing", false);


    }

	private final IBinder binder = new PlayerBinder();

	public class PlayerBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}

    public static synchronized Playlist getPlaylist() {
        return getPlaylist(null);
    }

    public static synchronized Playlist getPlaylist(@Nullable Playlist.PlaylistChangeListener argListener) {
        if (argListener != null) {
            sPlaylist.registerPlaylistChangeListener(argListener);
        }

        return sPlaylist;
    }

}
