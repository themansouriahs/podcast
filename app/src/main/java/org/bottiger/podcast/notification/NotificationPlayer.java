package org.bottiger.podcast.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.player.PlayerStateManager;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ImageLoaderUtils;

public class NotificationPlayer extends BroadcastReceiver {

    private static final String TAG = NotificationPlayer.class.getSimpleName();

    public static final String toggleAction = ApplicationConfiguration.packageName + ".TOGGLE";
    public static final String nextAction = ApplicationConfiguration.packageName + ".NEXT";
    public static final String clearAction = ApplicationConfiguration.packageName + ".CLEAR";
    public static final String playAction = ApplicationConfiguration.packageName + ".PLAY";
    public static final String pauseAction = ApplicationConfiguration.packageName + ".PAUSE";
    public static final String fastForwardAction = ApplicationConfiguration.packageName + ".FAST_FORWARD";
    public static final String rewindAction = ApplicationConfiguration.packageName + ".REWIND";
    public static final String muteAction = ApplicationConfiguration.packageName + ".MUTE";

    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;

    private boolean mStarted = false;

	private PlayerService mPlayerService;
	private IEpisode item;

    private Notification mNotification;
	private NotificationManagerCompat mNotificationManagerCompat = null;

    private PlaybackStateCompat mPlaybackState;
    private MediaMetadataCompat mMetadata;

	public static final int NOTIFICATION_PLAYER_ID = 4260;
    public static final int REQUEST_CODE = 413;

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state:" + state);

            if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
                    state.getState() == PlaybackStateCompat.STATE_NONE) {
                mPlayerService.dis_notifyStatus();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            Log.d(TAG, "Received new metadata: " + metadata);
        }

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                Log.e(TAG, "could not connect media controller");
            }
            super.onSessionDestroyed();
        }
    };

    public NotificationPlayer(@NonNull PlayerService service , @NonNull IEpisode item) throws RemoteException {
        this.mPlayerService = service;
        this.item = item;

        NotificationChannels.INSTANCE.createPlayerChannel(service);
        updateSessionToken();

        mNotificationManagerCompat = NotificationManagerCompat.from(service);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManagerCompat.cancelAll();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "received action: " + action);

        if (action.equals(toggleAction)) {
            mTransportControls.sendCustomAction(PlayerStateManager.ACTION_TOGGLE, new Bundle());
        }

        if (action.equals(nextAction)) {
            mTransportControls.skipToNext();
            return;
        }

        if (action.equals(clearAction)) {
            if (mPlayerService != null) {
                mPlayerService.halt();
            }
            return;
        }

        if (action.equals(playAction)) {
            mTransportControls.play();
            return;
        }

        if (action.equals(pauseAction)) {
            mTransportControls.pause();
            return;
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = mPlayerService.getSessionToken();
        if (mSessionToken == null && freshToken != null ||
                mSessionToken != null && !mSessionToken.equals(freshToken)) {

            mSessionToken = freshToken;
            if (mSessionToken != null) {
                mController = new MediaControllerCompat(mPlayerService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (mStarted) {
                    mController.registerCallback(mCb);
                }
            }
        }
    }

    public void show(@NonNull final IEpisode argItem) {
		show(PlayerService.isPlaying(), argItem);
	}

	public void show(final Boolean isPlaying, @NonNull final IEpisode argItem) {

        if (!argItem.equals(item)) {
            item = argItem;
        }

        showNotification(isPlaying);
	}

    public void setPlayerService(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
    }

    public void refresh() {
        showNotification(PlayerService.isPlaying());
    }

    public boolean isShowing() {
        return mStarted;
    }

	public IEpisode getItem() {
		return item;
	}

	public void setItem(IEpisode item) {
		this.item = item;
	}
	
	public static int getNotificationId() {
		return NOTIFICATION_PLAYER_ID;
	}

    @NonNull
    private NotificationCompat.Builder buildNotification(@NonNull Boolean isPlaying, @NonNull PlayerService argPlayerService, @Nullable Bitmap argBitmap) {

        int pause;
        int play;
        int next;
        int smallIcon;
        int clear = R.drawable.ic_clear_grey;

        if (isLight(0)) {
            pause = R.drawable.ic_play_arrow_black;
            play = R.drawable.ic_pause_black;
            next = R.drawable.ic_skip_next_black;
            smallIcon = R.drawable.ic_stat_notify;
        } else {
            pause = R.drawable.ic_play_arrow_white;
            play = R.drawable.ic_pause_white;
            next = R.drawable.ic_skip_next_white;
            smallIcon = R.drawable.ic_stat_notify_white;
        }

        String title = item.getTitle();
        String content = "";
        try {
            content = item.getSubscription(argPlayerService).getTitle();
        } catch (Exception e) {
            // Ignore
        }

        int toggleIconId = pause;
        if (isPlaying) toggleIconId = play;

        mNotificationManagerCompat = NotificationManagerCompat.from(mPlayerService);

        // Prepare intent which is triggered if the
        // notification is selected
        String pkg = mPlayerService.getPackageName();
        Intent toggleIntent = new Intent(NotificationPlayer.toggleAction).setPackage(pkg);
        Intent nextIntent = new Intent(NotificationPlayer.nextAction).setPackage(pkg);
        Intent clearIntent = new Intent(NotificationPlayer.clearAction).setPackage(pkg);
        Intent playIntent = new Intent(NotificationPlayer.playAction).setPackage(pkg);
        Intent pauseIntent = new Intent(NotificationPlayer.pauseAction).setPackage(pkg);

        PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(mPlayerService, REQUEST_CODE, toggleIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(mPlayerService, REQUEST_CODE, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingClearIntent = PendingIntent.getBroadcast(mPlayerService, REQUEST_CODE, clearIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(mPlayerService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(mPlayerService, REQUEST_CODE, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // yes, the play/pause icons are inverted for now
        NotificationCompat.Action actionToggle = new NotificationCompat.Action(toggleIconId, "Toggle", pendingToggleIntent);
        NotificationCompat.Action actionPlay = new NotificationCompat.Action(pause, "Play", pendingPlayIntent);
        NotificationCompat.Action actionPause = new NotificationCompat.Action(play, "Pause", pendingPauseIntent);
        NotificationCompat.Action actionNext = new NotificationCompat.Action(next, "Next", pendingNextIntent);
        NotificationCompat.Action actionClear = new NotificationCompat.Action(clear, "Clear", pendingClearIntent);

        android.support.v4.media.app.NotificationCompat.MediaStyle mediaStyle = new android.support.v4.media.app.NotificationCompat.MediaStyle();
        mediaStyle.setShowActionsInCompactView(1, 2);
        mediaStyle.setShowCancelButton(true);
        MediaSessionCompat.Token mediaSessionToken = mPlayerService.getPlayerStateManager().getToken();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mPlayerService, NotificationChannels.CHANNEL_ID_PLAYER);
        mBuilder.setSmallIcon(smallIcon);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(content);

        mBuilder.setDeleteIntent(pendingClearIntent);

        // Should I use this? I'm not sure.
        mBuilder.setStyle(mediaStyle
                .setMediaSession(mediaSessionToken));
        mBuilder.addAction(actionNext);
        mBuilder.addAction(actionClear);
        mBuilder.addAction(actionToggle);
        //mBuilder.setProgress(100,  50, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        mBuilder.setCategory("CATEGORY_TRANSPORT"); // Media transport control for playback
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        mBuilder.setOnlyAlertOnce(true);

        Intent resultIntent = new Intent(mPlayerService, MainActivity.class);

        // Sets a custom content view for the notification, including an image button.
        RemoteViews layout = new RemoteViews(mPlayerService.getPackageName(), R.layout.notification);
        layout.setTextViewText(R.id.notification_title, title);
        layout.setTextViewText(R.id.notification_content, content);


        int visibility = isPlaying ? View.INVISIBLE : View.VISIBLE;
        layout.setViewVisibility(R.id.clear_button, visibility);

        if (argBitmap != null && !argBitmap.isRecycled()) {
            Log.d(TAG, "Creating notification with bitmap");

            mBuilder.setLargeIcon(argBitmap);
            layout.setImageViewBitmap(R.id.icon, argBitmap);
        }

        layout.setOnClickPendingIntent(R.id.play_pause_button,pendingToggleIntent);
        //layout.setOnClickPendingIntent(R.id.next_button,pendingNextIntent);
        layout.setOnClickPendingIntent(R.id.clear_button,pendingClearIntent);

        //PlayerStatusListener.registerImageView(, mPlayerService);

        layout.setImageViewResource(R.id.play_pause_button, toggleIconId);
        //layout.setImageViewResource(R.id.next_button, next);
        layout.setImageViewResource(R.id.clear_button, clear);

        mBuilder.setContent(layout);


        Intent intent = new Intent(mPlayerService.getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mPlayerService.getApplicationContext(), 0, intent, 0);
        mBuilder.setContentIntent(pendingIntent);

        // Set in the constructor
        //mBuilder.setChannelId(NotificationChannels.CHANNEL_ID_PLAYER);
        mBuilder.setSound(null);

        return mBuilder;
    }

    private void showNotification(final boolean isPlaying) {
        String url = item.getArtwork(mPlayerService);

        if (TextUtils.isEmpty(url)) {
            // NOTIFICATION_PLAYER_ID allows you to update the notification later on.
            displayNotification(isPlaying, null);
        } else {
            ImageLoaderUtils.getGlide(mPlayerService, url)
                    .into(new SimpleTarget<Bitmap>(512, 512) {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            displayNotification(isPlaying, resource);
                        }
                    });
        }
    }

    public void hide() {
        mStarted = false;
        mController.unregisterCallback(mCb);

        if (mNotificationManagerCompat != null)
            mNotificationManagerCompat.cancel(NOTIFICATION_PLAYER_ID);

        mPlayerService.stopForeground(true);
    }

    private void displayNotification(boolean isPlaying, @Nullable Bitmap argBitmap) {
        NotificationCompat.Builder builder = buildNotification(isPlaying, mPlayerService, argBitmap);
        mNotification = builder.build();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationPlayer.toggleAction);
        filter.addAction(NotificationPlayer.clearAction);
        filter.addAction(NotificationPlayer.nextAction);
        filter.addAction(NotificationPlayer.pauseAction);
        filter.addAction(NotificationPlayer.playAction);
        filter.addAction(NotificationPlayer.rewindAction);
        filter.addAction(NotificationPlayer.fastForwardAction);
        mPlayerService.registerReceiver(this, filter);

        if (isPlaying) {
            mPlayerService.startForeground(getNotificationId(), mNotification);
        }
        mNotificationManagerCompat.notify(NOTIFICATION_PLAYER_ID, mNotification);

        mStarted = true;
    }

    /**
     * http://stackoverflow.com/questions/4679715/is-there-a-way-to-tell-if-a-html-hex-colour-is-light-or-dark
     *
     * A more natural approach might be using the HSL colorspace. You can use the third component ("lightness") to figure out h
     * ow light a colour is. That will work for most purposes.
     *
     * Plenty of descriptions out there by googling. Basically you take the colour component with the highest value
     * (let's say it's red) and the one with the lowest (say, green). In this case:
     *
     * L = (red + green) / (255*2.0)
     *
     * Assuming you extracted your colours as values from 0 to 255. You'll get a value between 0 and 1. A light colour
     * could be any colour with a lightness above a certain arbitrary value (for instance, 0.6).
     */
    private static boolean isLight(int argColor) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

}
