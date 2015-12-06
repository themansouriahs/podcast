package org.bottiger.podcast.notification;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.NotificationReceiver;
import org.bottiger.podcast.service.PlayerService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

public class NotificationPlayer {

    private static final String TAG = "NotificationPlayer";

	private PlayerService mPlayerService;
	private IEpisode item;

    private Notification mNotification;
	private NotificationManager mNotificationManager = null;

	public static final int NOTIFICATION_PLAYER_ID = 4260;
	
	public NotificationPlayer(@NonNull PlayerService argPlayerService, @NonNull IEpisode item) {
		super();
		this.mPlayerService = argPlayerService;
		this.item = item;
	}

    @Nullable
	public void show(@NonNull final IEpisode argItem) {
		show(true, argItem);
	}

    @Nullable
	public void show(final Boolean isPlaying, @NonNull final IEpisode argItem) {

        if (!argItem.equals(item)) {
            item = argItem;
        }

        if (item == null)
            return;

        //mPlayerService.startForeground(getNotificationId(), mNotification);

        showNotification(isPlaying);
	}

    public void setPlayerService(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
    }

    public void refresh() {
        PlayerService ps = mPlayerService;
        if (ps != null) {
            showNotification(ps.isPlaying());
        }
    }
	
	public void hide() {
        if (mNotificationManager != null)
		    mNotificationManager.cancel(NOTIFICATION_PLAYER_ID);
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
            content = item.getSubscription().getTitle();
        } catch (Exception e) {
            // Ignore
        }

        int toggleIconId = pause;
        if (isPlaying) toggleIconId = play;

        // Prepare intent which is triggered if the
        // notification is selected
        Intent toggleIntent = new Intent(NotificationReceiver.toggleAction);
        Intent nextIntent = new Intent(NotificationReceiver.nextAction);
        Intent clearIntent = new Intent(NotificationReceiver.clearAction);
        Intent playIntent = new Intent(NotificationReceiver.playAction);
        Intent pauseIntent = new Intent(NotificationReceiver.pauseAction);

        PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(mPlayerService, 0, toggleIntent, 0);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(mPlayerService, 0, nextIntent, 0);
        PendingIntent pendingClearIntent = PendingIntent.getBroadcast(mPlayerService, 0, clearIntent, 0);
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(mPlayerService, 0, pauseIntent, 0);
        PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(mPlayerService, 0, playIntent, 0);

        // yes, the play/pause icons are inverted for now
        NotificationCompat.Action actionToggle = new NotificationCompat.Action(toggleIconId, "Toggle", pendingToggleIntent);
        NotificationCompat.Action actionPlay = new NotificationCompat.Action(pause, "Play", pendingPlayIntent);
        NotificationCompat.Action actionPause = new NotificationCompat.Action(play, "Pause", pendingPauseIntent);
        NotificationCompat.Action actionNext = new NotificationCompat.Action(next, "Next", pendingNextIntent);
        NotificationCompat.Action actionClear = new NotificationCompat.Action(clear, "Clear", pendingClearIntent);

        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();
        mediaStyle.setShowActionsInCompactView(1, 2);
        mediaStyle.setShowCancelButton(true);
        MediaSessionCompat.Token mediaSessionToken = mPlayerService.getPlayerStateManager().getToken();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mPlayerService);
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


        /*
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mPlayerService);
        // Adds the back stack for the Intent (but not the Intent itself)
        //stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        */
        Intent intent = new Intent(mPlayerService.getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mPlayerService.getApplicationContext(), 0, intent, 0);
        mBuilder.setContentIntent(pendingIntent);

        mNotificationManager =
                (NotificationManager) mPlayerService.getSystemService(Context.NOTIFICATION_SERVICE);

        return mBuilder;
    }

    private void showNotification(final boolean isPlaying) {
        String url = item.getArtwork();
        if (TextUtils.isEmpty(url)) {

            // NOTIFICATION_PLAYER_ID allows you to update the notification later on.
            displayNotification(isPlaying, null);
        } else {
            Glide.with(mPlayerService)
                    .load(url)
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>(512, 512) {
                        @Override
                        public void onResourceReady(Bitmap argBitmap, GlideAnimation anim) {
                            displayNotification(isPlaying, argBitmap);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            return;
                        }
                    });
        }
    }

    private void displayNotification(boolean isPlaying, @Nullable Bitmap argBitmap) {
        NotificationCompat.Builder builder = buildNotification(isPlaying, mPlayerService, argBitmap);
        mNotification = builder.build();
        mPlayerService.startForeground(getNotificationId(), mNotification);
        mNotificationManager.notify(NOTIFICATION_PLAYER_ID, mNotification);
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
