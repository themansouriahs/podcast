package org.bottiger.podcast.service;

import org.bottiger.podcast.Player.LegacyRemoteController;
import org.bottiger.podcast.Player.MetaDataControllerWrapper;
import org.bottiger.podcast.Player.PlayerHandler;
import org.bottiger.podcast.Player.PlayerPhoneListener;
import org.bottiger.podcast.Player.PlayerStateManager;
import org.bottiger.podcast.Player.SoundWavesPlayer;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.MediaCast.IMediaCast;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.HeadsetReceiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import javax.annotation.Nullable;

/**
 * The service which handles the audio extended_player. This is responsible for playing
 * including controlling the playback. Play, pause, stop etc.
 * 
 * @author Arvid Böttiger
 */
public class PlayerService extends Service implements
		AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "PlayerService";

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

	/** Which action to perform when a track ends */
	public static enum NextTrack {
		NONE, NEW_TRACK, NEXT_IN_PLAYLIST
	}

	private static NextTrack nextTrack = NextTrack.NEXT_IN_PLAYLIST;
	
	private Playlist mPlaylist;

	private SoundWavesPlayer mPlayer = null;
    private MediaController mController;

    private MetaDataControllerWrapper mMetaDataControllerWrapper;
    private PlayerStateManager mPlayerStateManager;
    private LegacyRemoteController mLegacyRemoteController;

    // Google Cast
    private IMediaCast mMediaCast;


    private NotificationManager mNotificationManager;
    @Nullable private NotificationPlayer mNotificationPlayer;
    @Nullable private MediaSessionManager mMediaSessionManager;

	// AudioManager
	private AudioManager mAudioManager;
	private ComponentName mControllerComponentName;

	private IEpisode mItem = null;
    private boolean mResumePlayback = false;

    private final String LOCK_NAME = "SoundWavesWifiLock";
    WifiManager.WifiLock wifiLock;

    private PlayerHandler mPlayerHandler;

	/**
	 * Phone state listener. Will pause the playback when the phone is ringing
	 * and continue it afterwards
	 */
	private PhoneStateListener mPhoneStateListener = new PlayerPhoneListener(this);

	public void startAndFadeIn() {
        mPlayerHandler.sendEmptyMessageDelayed(PlayerHandler.FADEIN, 10);
	}

    public SoundWavesPlayer getPlayer() {
        return mPlayer;
    }

	@Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "PlayerService started");

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, LOCK_NAME);

        mPlaylist = new Playlist(this);
		mPlaylist.populatePlaylistIfEmpty();
		SoundWaves.getBus().register(mPlaylist);

        mPlayerHandler = new PlayerHandler(this);
		
		mPlayer = new SoundWavesPlayer(this);
		mPlayer.setHandler(mPlayerHandler);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		this.mControllerComponentName = new ComponentName(this,
				HeadsetReceiver.class);
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
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
                mController.getTransportControls().fastForward();
            } else if (action.equalsIgnoreCase(ACTION_REWIND)) {
                mController.getTransportControls().rewind();
            } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
                mController.getTransportControls().skipToPrevious();
            } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
                mController.getTransportControls().skipToNext();
            } else if (action.equalsIgnoreCase(ACTION_STOP)) {
                mController.getTransportControls().stop();
            }
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
        handleIntent(intent);
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);

        mMetaDataControllerWrapper.register(this);

        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

	@Override
	public void onDestroy() {
		SoundWaves.getBus().unregister(mPlaylist);
        super.onDestroy();
		if (mPlayer != null) {
			mPlayer.release();
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
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

        mNotificationPlayer.setmPlayerService(this);
		return mNotificationPlayer.show();
    }

	public void playNext() {
        IEpisode item = getCurrentItem();
        IEpisode nextItem = mPlaylist.getNext();

		if (item != null && item instanceof FeedItem) {
            ((FeedItem)item).trackEnded(getContentResolver());
        }

        if (nextItem == null) {
            stop();
            return;
        }

		play(nextItem.getUrl().toString());
        mMetaDataControllerWrapper.updateState(nextItem, true, true);
        mPlaylist.removeItem(0);
        mPlaylist.notifyPlaylistChanged();
	}

	public void play(String argEpisodeURL) {

		// Pause the current episode in order to save the current state
		if (mPlayer.isPlaying())
			mPlayer.pause();

		if (mItem != null) {
			if ((mItem.getUrl().toString() == argEpisodeURL) && mPlayer.isInitialized()) {
				if (!mPlayer.isPlaying()) {
					start();
				}
				return;
			}

			if (mPlayer.isPlaying()) {
                mItem.setOffset(getContentResolver(), mPlayer.position());
				stop();
			}
		}

		IEpisode oldItem = mItem;

		mItem = FeedItem.getByURL(getContentResolver(), argEpisodeURL);

		if (mItem == null)
			return;


		// Removed the current top episode from the playlist if it has been started
		if (oldItem != null && !oldItem.equals(mItem)) {
			if (oldItem instanceof FeedItem) {
				FeedItem oldFeedItem = (FeedItem)oldItem;
				if (oldFeedItem.getOffset() > 0) {
					oldFeedItem.markAsListened();
					oldFeedItem.update(getContentResolver());

					int pos = mPlaylist.getPosition(oldItem);
					if (pos > 0) {
						mPlaylist.removeItem(pos);
					}
				}
			}
		}

        boolean isFeedItem = false;
        if (mItem instanceof FeedItem) {
            isFeedItem = true;
        }
        final FeedItem feedItem = isFeedItem ? (FeedItem)mItem : null;

		String dataSource = mItem.isDownloaded() ? feedItem.getAbsolutePath()
				: mItem.getUrl().toString();

		int offset = mItem.getOffset() < 0 ? 0 : (int) mItem.getOffset();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        if (offset == 0 && prefs.getBoolean("pref_stream_proxy", false))
            dataSource = HTTPDService.proxyURL(mItem.getUrl().toString());

		mPlayer.setDataSourceAsync(dataSource, offset);

        IEpisode item = getCurrentItem();
        if (item != null) {
            mMetaDataControllerWrapper.updateState(item, true, true);
        }

        if (isFeedItem) {
            new Thread(new Runnable() {
                public void run() {
                    if (feedItem.priority != 1)
                        feedItem.setPriority(null, getApplication());
                    feedItem.update(getContentResolver());
                }
            }).start();
        }
	    
	}

    /**
     *
     * @return True of the songs start to play
     */
	public boolean toggle(@NonNull IEpisode argEpisode) {
        IEpisode item = getCurrentItem();
		if (!mPlayer.isPlaying() || (item != null && !argEpisode.getUrl().equals(item.getUrl()))) {
			play(argEpisode.getUrl().toString());
            return true;
		} else {
			pause();
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
		if (!mPlayer.isPlaying()) {
            takeWakelock(mPlayer.isSteaming());
			mPlayer.start();
			mMetaDataControllerWrapper.updateState(mItem, true, false);
		}
	}

	public void pause() {
		if (!mPlayer.isPlaying()) {
			return;
		}

		if ((mItem != null)) {
            if (mItem instanceof FeedItem) {
                ((FeedItem)mItem).setOffset(getContentResolver(), mPlayer.position());
            }
		}
		dis_notifyStatus();

		mPlayer.pause();
        mMetaDataControllerWrapper.updateState(mItem, false, false);
        releaseWakelock();
	}

	public void stop() {
		pause();
		mPlayer.stop();
		mItem = null;
		dis_notifyStatus();
	}

	public void halt() {
		mPlayer.stop();
		stopForeground(true);
	}

	public boolean isInitialized() {
		return mPlayer.isInitialized();
	}

	public boolean isPlaying() {
        if (!mPlayer.isInitialized())
            return false;

		return mPlayer.isPlaying();
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

	public IEpisode getCurrentItem() {
		return mItem;
	}

	/**
	 * @return The ID of the next episode in the playlist
	 */
    @Nullable
	public IEpisode getNextId() {
		IEpisode next = mPlaylist.nextEpisode();
		if (next != null)
			return next;
		return null;
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

    public void setMediaCast(IMediaCast mMediaCast) {
        this.mMediaCast = mMediaCast;
        mMediaCast.registerStateChangedListener(getPlayer());
    }

    /**
     * Remove wakelocks
     */
    public void releaseWakelock() {
        //stopForeground(true);
        //mPlayer.release();

        if (wifiLock.isHeld())
            wifiLock.release();
    }

	private final IBinder binder = new PlayerBinder();

	public class PlayerBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}

    public Playlist getPlaylist() {
        return mPlaylist;
    }

}
