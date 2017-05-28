package org.bottiger.podcast.service;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.telephony.PhoneStateListener;
import android.util.Log;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.player.PlayerPhoneListener;
import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.chapter.ChapterUtil;
import org.bottiger.podcast.widgets.SoundWavesWidgetProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.processors.FlowableProcessor;

import static org.bottiger.podcast.player.PlayerStateManager.ACTION_TOGGLE;

/**
 * The service which handles the audio extended_player. This is responsible for playing
 * including controlling the playback. Play, pause, stop etc.
 * 
 * @author Arvid Böttiger
 */
public class PlayerService extends MediaBrowserServiceCompat implements
		AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = PlayerService.class.getSimpleName();

	/** Which action to perform when a track ends */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({NONE, NEW_TRACK, NEXT_IN_PLAYLIST})
	public @interface NextTrack {}
	public static final int NONE = 1;
	public static final int NEW_TRACK = 2;
	public static final int NEXT_IN_PLAYLIST = 3;

	private static @PlayerService.NextTrack int nextTrack = NEXT_IN_PLAYLIST;

    private MediaControllerCompat mController;

    private NotificationManager mNotificationManager;
    @Nullable private MediaSessionManager mMediaSessionManager;

	// AudioManager
	private AudioManager mAudioManager;
	private ComponentName mControllerComponentName;

	private IEpisode mItem = null;
    private boolean mResumePlayback = false;

    private final String LOCK_NAME = "SoundWavesWifiLock";
    WifiManager.WifiLock wifiLock;

	@NonNull
	private PlayerStateManager mPlayerStateManager;

	@NonNull
	private PlayerStatusObservable mPlayerStatusObservable;


	/**
	 * Phone state listener. Will pause the playback when the phone is ringing
	 * and continue it afterwards
	 */
	private PhoneStateListener mPhoneStateListener;

    public GenericMediaPlayerInterface getPlayer() {
        return SoundWaves.getAppContext(this).getPlayer();
    }

	private static PlayerService sInstance = null;

	@Nullable
	@Deprecated
	public static PlayerService getInstance() {
		return sInstance;
	}

	@RequiresPermission(Manifest.permission.READ_PHONE_STATE)
	@Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "PlayerService started");

		sInstance = this;

        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, LOCK_NAME);

		mPlayerStateManager = SoundWaves.getAppContext(this).getPlayerStateManager();
		mPlayerStateManager.setService(this);

		mPlayerStatusObservable = new PlayerStatusObservable(this);

		SoundWaves.getAppContext(this).getPlayer().setPlayerService(this);

		try {
			mController = new MediaControllerCompat(getApplicationContext(), mPlayerStateManager.getToken()); // .fromToken( mSession.getSessionToken() );
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		setSessionToken(mPlayerStateManager.getToken());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        }

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		mPhoneStateListener = new PlayerPhoneListener(this);

		this.mControllerComponentName = new ComponentName(this,
				HeadsetReceiver.class);
		this.mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
			mAudioManager.abandonAudioFocus(this);
			// Stop playback
			//stop();
			pause();
		}
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");

		boolean sendToTransportControls = handleIncomingActions(intent);

		if (!sendToTransportControls) {
			MediaButtonReceiver.handleIntent(mPlayerStateManager.getSession(), intent);
		}

        return START_STICKY;
    }

	private boolean handleIncomingActions(Intent playbackAction) {
		if (playbackAction == null || playbackAction.getAction() == null) {
			return false;
		}

		boolean wasHandeled = false;
		String actionString = playbackAction.getAction();
		if (actionString.equalsIgnoreCase(NotificationPlayer.toggleAction)) {
			mPlayerStateManager.onCustomAction(ACTION_TOGGLE, null);
			wasHandeled = true;
		} else if (actionString.equalsIgnoreCase(NotificationPlayer.playAction)) {
			mPlayerStateManager.onPlay();
			wasHandeled = true;
		} else if (actionString.equalsIgnoreCase(NotificationPlayer.pauseAction)) {
			mPlayerStateManager.onPause();
			wasHandeled = true;
		} else if (actionString.equalsIgnoreCase(NotificationPlayer.nextAction)) {
			mPlayerStateManager.onSkipToNext();
			wasHandeled = true;
		} else if (actionString.equalsIgnoreCase(NotificationPlayer.rewindAction)) {
			mPlayerStateManager.onRewind();
			wasHandeled = true;
		} else if (actionString.equalsIgnoreCase(NotificationPlayer.fastForwardAction)) {
			mPlayerStateManager.onFastForward();
			wasHandeled = true;
		}

		return wasHandeled;
	}

	@Override
	public void onDestroy() {
		sInstance = null;
        super.onDestroy();
		GenericMediaPlayerInterface player = getPlayer();
		if (player != null) {
			player.release();
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@android.support.annotation.Nullable
	@Override
	public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @android.support.annotation.Nullable Bundle rootHints) {
		return new BrowserRoot(getString(R.string.app_name), null);
	}

	@Override
	public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
		result.sendResult(new LinkedList<MediaBrowserCompat.MediaItem>());
	}

	/**
	 * Hide the notification
	 */
    public void dis_notifyStatus() {

        if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NotificationPlayer.NOTIFICATION_PLAYER_ID);

		getPlayer().removeNotificationPlayer();
	}

	/**
	 * Display a notification with the current podcast
	 */
    public void notifyStatusChanged() {

		SoundWavesWidgetProvider.updateAllWidgets(this, mPlayerStateManager.getState());

		getPlayer().updateNotificationPlayer();

        mPlayerStatusObservable.startProgressUpdate(isPlaying());
    }

	public void playNext() {
        IEpisode item = getCurrentItem();
        Playlist playlist = SoundWaves.getAppContext(this).getPlaylist();

        IEpisode nextItem = playlist.getNext();

		if (item != null && item instanceof FeedItem) {
            ((FeedItem)item).trackEnded(getContentResolver());
        }

        if (nextItem == null) {
            stop();
            return;
        }

        playlist.removeFirst();
        playlist.notifyPlaylistChanged();

		play(nextItem, false);
	}

	public void play() {
		GenericMediaPlayerInterface player = getPlayer();

		if (player == null)
			return;

		if (player.isPlaying())
			return;

        if (mItem == null)
			setCurrentItem(SoundWaves.getAppContext(this).getPlaylist().first());

        if (mItem == null)
            return;

		play(mItem, true);
	}

	/**
	 *
	 * @param argEpisode The episode to be played
	 * @return True if the episode is being played
     */
	public boolean play(@NonNull IEpisode argEpisode, boolean triggeredManually) {

		GenericMediaPlayerInterface player = getPlayer();

		// Pause the current episode in order to save the current state
		if (player.isPlaying()) {
			player.pause();
		}

		IEpisode currentItem = getCurrentItem();

		if (currentItem != null) {
			if (argEpisode.equals(currentItem) && player.isInitialized()) {
				if (!player.isPlaying()) {
					start();
				}
				return true;
			}
		}

		currentItem = setCurrentItem(argEpisode);
        Playlist playlist = SoundWaves.getAppContext(this).getPlaylist();

        if (triggeredManually)
            playlist.setAsFrist(currentItem);

        float speed = getPlaybackSpeed(this, currentItem);

        player.setPlaybackSpeed(speed);
        player.setDataSourceAsync(currentItem);

		updateMetadata(currentItem);

		return true;
	}

	private void updateMetadata(IEpisode item) {
		notifyStatusChanged();
		mPlayerStateManager.updateMedia(item);
	}

    /**
     *
     * @return True of the songs start to play
     */
	@MainThread
	public boolean toggle(@NonNull IEpisode argEpisode) {

        if (getPlayer().isPlaying()) {
            IEpisode item = getCurrentItem();
            if (argEpisode.equals(item)) {
                pause();
                return false;
            }
        }

		URL url = argEpisode.getUrl();

		if (url == null) {
			ISubscription subscription = argEpisode.getSubscription(this);
			if (subscription != null)
				VendorCrashReporter.report("Malform episode", "subscription: " + subscription.toString());

			Log.wtf(TAG, "Malform episode");
			return false;
		}

        return play(argEpisode, true);
	}

	public boolean toggle() {

		Playlist playlist = SoundWaves.getAppContext(this).getPlaylist(true);
		IEpisode episode = playlist.first();

		if (episode == null) {
			Log.wtf(TAG, "episode null");
			return false;
		}

		toggle(episode);

		return true;
	}

	private void start() {
		if (!getPlayer().isPlaying()) {
			if (mItem == null)
				setCurrentItem(SoundWaves.getAppContext(this).getPlaylist().first());

			if (mItem == null)
				return;

			getPlayer().setVolume(1.0f);

            takeWakelock(getPlayer().isSteaming());
			SoundWaves.getAppContext(this).getPlaylist().setAsFrist(mItem);
			getPlayer().start();

			notifyStatusChanged();
		}
	}

	public void pause() {

		stopForeground(false);

		GenericMediaPlayerInterface player = getPlayer();

		if (!player.isPlaying()) {
			return;
		}

		if (mItem != null) {
            mItem.setOffset(player.getCurrentPosition());
		}

		player.pause();
        releaseWakelock();

		notifyStatusChanged();
	}

	public void stop() {
		pause();
		getPlayer().stop();
		mItem = null;
		dis_notifyStatus();
	}

	public void halt() {
        dis_notifyStatus();
		getPlayer().pause();
	}

	public static boolean isPlaying() {

		if (sInstance == null)
			return false;

		GenericMediaPlayerInterface player = sInstance.getPlayer();

        if (!player.isInitialized())
            return false;

		return player.isPlaying();
	}

	public long seek(long offset) {
		offset = offset < 0 ? 0 : offset;

		mItem.setOffset(offset);

		notifyStatusChanged();

		return getPlayer().seekTo(offset);
	}

	public long position() {
		return getPlayer().getCurrentPosition();
	}

	public long duration() {
		return getPlayer().getCurrentPosition();
	}

	public IEpisode setCurrentItem(@Nullable IEpisode argEpisode) {
		mItem = argEpisode;
		return mItem;
	}

	@Nullable
	public static IEpisode getCurrentItem(@android.support.annotation.Nullable Context argContext) {
		PlayerService ps = PlayerService.getInstance();

		if (ps == null)
			return null;

		IEpisode currentItem = ps.getCurrentItemInternal();

		if (currentItem != null) {
			return currentItem;
		}

		if (argContext == null) {
			return null;
		}

		currentItem = ps.setCurrentItem(SoundWaves.getAppContext(argContext).getPlaylist().first());
		return currentItem;
	}

	@Nullable
	public static IEpisode getCurrentItem() {
		return getCurrentItem(null);
	}

	@Nullable
	private IEpisode getCurrentItemInternal() {
		return mItem;
	}

	/**
	 * @return The ID of the next episode in the playlist
	 */
    @Nullable
	public IEpisode getNext() {
		IEpisode next = SoundWaves.getAppContext(this).getPlaylist().nextEpisode();
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
	public static @NextTrack int getNextTrack() {
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


		getPlayer().updateNotificationPlayer();

        if (isSteaming && mWifi.isConnected()) {
            wifiLock.acquire();
        }
    }

    /**
     * Remove wakelocks
     */
    public void releaseWakelock() {

        if (wifiLock.isHeld())
            wifiLock.release();
    }

	@Deprecated
    public Playlist getPlaylist() {
        return SoundWaves.getAppContext(this).getPlaylist();
    }

	public PlayerStateManager getPlayerStateManager() {
		return mPlayerStateManager;
	}

	public void updateChapter(long lastPosition) {

		FlowableProcessor<Integer> chapterProcessor = SoundWaves.getAppContext(this).getChapterProcessor();
		if (!chapterProcessor.hasSubscribers())
			return;

		IEpisode episode = SoundWaves.getAppContext(this).getPlaylist().first();
		long currentPosition = position();
		Integer lastChapterIndex = ChapterUtil.getCurrentChapterIndex(episode, lastPosition);
		Integer currentChapterIndex = ChapterUtil.getCurrentChapterIndex(episode, currentPosition);

		if (currentChapterIndex != null && !currentChapterIndex.equals(lastChapterIndex)) {
			chapterProcessor.onNext(currentChapterIndex);
		}
	}

	public static float getPlaybackSpeed(@NonNull Context argContext, @NonNull IEpisode argEpisode) {
		ISubscription subscription = argEpisode.getSubscription(argContext);
		float speed = PlaybackSpeed.DEFAULT;

		float globalSpeed = PlaybackSpeed.globalPlaybackSpeed(argContext);
		if (globalSpeed != PlaybackSpeed.UNDEFINED) {
			speed = globalSpeed;
		}

		if (subscription instanceof Subscription) {
			speed = ((Subscription) subscription).getPlaybackSpeed();
		}

		return speed;
	}
}
