package org.bottiger.podcast.notification;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.Player.PlayerStateManager;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.receiver.NotificationReceiver;
import org.bottiger.podcast.service.PlayerService;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class NotificationPlayer {
	
	private Context mContext;
	private FeedItem item;
    private Bitmap mArtwork;

    private PlayerService mPlayerService;
    private Notification mNotification;

    private Target loadtarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            // do something with the Bitmap
            mArtwork = bitmap;
            refresh();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            refresh();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };
	
	private NotificationManager mNotificationManager = null;
	private static int mId = 4260;
	
	public NotificationPlayer(@NonNull Context context, @NonNull FeedItem item) {
		super();
		this.mContext = context;
		this.item = item;
        item.getArtworAsync(mContext, loadtarget);
	}

    @Nullable
    public Notification getNotification() {
        return mNotification;
    }

    @Nullable
	public Notification show() {
		return show(true);
	}

    @Nullable
	public Notification show(Boolean isPlaying) {

		// mId allows you to update the notification later on.
        NotificationCompat.Builder builder = buildNotification(isPlaying, mPlayerService, mArtwork);

        if (builder == null)
            return null;

        mNotification = builder.build();

        if (mNotification == null)
            return null;

		//mNotificationManager.notify(mId, not);
        if (mContext instanceof PlayerService) {
            ((PlayerService) mContext).startForeground(getNotificationId(), mNotification);
        }

        mNotificationManager.notify(mId, mNotification);

		return mNotification;
	}

    public void setPlayerService(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
    }

    public void refresh() {
        PlayerService ps = mPlayerService;
        if (ps != null) {
            NotificationCompat.Builder notificationBuilder = buildNotification(ps.isPlaying(), mPlayerService, mArtwork);

            if (notificationBuilder == null)
                return;

            mNotification = notificationBuilder.build();
            mNotificationManager.notify(mId, mNotification);
        }
    }
	
	public void hide() {
        if (mNotificationManager != null)
		    mNotificationManager.cancel(mId);
	}

	public FeedItem getItem() {
		return item;
	}

	public void setItem(FeedItem item) {
		this.item = item;
        item.getArtworAsync(mContext, loadtarget);
	}
	
	public static int getNotificationId() {
		return mId;
	}

    @TargetApi(20)
    private Notification.Action generateAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent( mContext.getApplicationContext(), PlayerService.class );
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(mContext.getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder( icon, title, pendingIntent ).build();
    }


	private NotificationCompat.Builder buildNotification(@NonNull Boolean isPlaying, @NonNull PlayerService argPlayerService, @Nullable Bitmap icon) {
        return customStyleNotification(isPlaying, argPlayerService, icon);
	}

    @Nullable
    private NotificationCompat.Builder customStyleNotification(@NonNull Boolean isPlaying, @NonNull PlayerService argPlayerService, @Nullable Bitmap icon) {

        if (item == null) {
            return null;
        }

        String bitmapstring = icon == null ? "null" : "present";
        Log.d("NotificationPlayer", "Build notification, icon => " + bitmapstring);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(item.title)
                        .setContentText(item.sub_title)
                        .setLargeIcon(icon);

        mBuilder.setOnlyAlertOnce(true);

        Intent resultIntent = new Intent(mContext, NotificationReceiver.class);

        // Sets a custom content view for the notification, including an image button.
        RemoteViews layout = new RemoteViews(mContext.getPackageName(), R.layout.notification);
        layout.setTextViewText(R.id.notification_title, item.title);
        layout.setTextViewText(R.id.notification_content, item.sub_title);
        layout.setImageViewBitmap(R.id.icon, icon);

        // Prepare intent which is triggered if the
        // notification is selected
        Intent toggleIntent = new Intent(NotificationReceiver.toggleAction);
        Intent nextIntent = new Intent(NotificationReceiver.nextAction);

        PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(mContext, 0, toggleIntent, 0);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(mContext, 0, nextIntent, 0);

        layout.setOnClickPendingIntent(R.id.play_pause_button,pendingToggleIntent);
        //layout.setOnClickPendingIntent(R.id.next_button,pendingNextIntent);

        //PlayerStatusListener.registerImageView(, mContext);

        int srcId = R.drawable.ic_play_arrow_black;
        if (isPlaying) srcId = R.drawable.ic_pause_black;

        layout.setImageViewResource(R.id.play_pause_button, srcId);

        mBuilder.setContent(layout);


        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        return mBuilder;
    }

    @Nullable
    @TargetApi(21)
    private Notification.Builder mediaStyleNotification(@NonNull Boolean isPlaying, @NonNull PlayerService argPlayerService, @Nullable Bitmap icon) {

        if (item == null) {
            return null;
        }

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        //Bitmap icon = new BitmapProvider(mContext, item).createBitmapFromMediaFile(128, 128);

        PlayerStateManager playerStateManager = argPlayerService.getPlayerStateManager();
        Notification.MediaStyle mediaStyle = new Notification.MediaStyle()
                .setMediaSession(playerStateManager.getToken())
                .setShowActionsInCompactView(0);

        Notification.Builder mBuilder = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.soundwaves) //.setSmallIcon(R.drawable.soundwaves)
                .setLargeIcon(icon)     // setMediaSession(token, true)
                .setContentTitle(item.title)     // these three lines are optional
                .setContentText(item.sub_title)   // if you use
                        //.setMediaSession(PlayerService.SESSION_ID, true) // , true)
                .setStyle(mediaStyle);

        mBuilder.setOngoing(true);
        mBuilder.setShowWhen(false);
        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);


        mBuilder.addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", PlayerService.ACTION_PAUSE));
        mBuilder.addAction(generateAction(android.R.drawable.ic_media_play, "Play", PlayerService.ACTION_PLAY));
        mBuilder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", PlayerService.ACTION_PLAY));

        Intent resultIntent = new Intent(mContext, NotificationReceiver.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder;
    }

	
}
