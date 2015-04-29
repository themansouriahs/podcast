package org.bottiger.podcast.notification;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.receiver.NotificationReceiver;
import org.bottiger.podcast.service.PlayerService;

import android.annotation.TargetApi;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class NotificationPlayer {
	
	private PlayerService mPlayerService;
	private FeedItem item;
    private Bitmap mArtwork;

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
	
	public NotificationPlayer(@NonNull PlayerService argPlayerService, @NonNull FeedItem item) {
		super();
		this.mPlayerService = argPlayerService;
		this.item = item;
        item.getArtworAsync(mPlayerService, loadtarget);
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

		mPlayerService.startForeground(getNotificationId(), mNotification);

        mNotificationManager.notify(mId, mNotification);

		return mNotification;
	}

    public void setmPlayerService(@NonNull PlayerService argPlayerService) {
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
        item.getArtworAsync(mPlayerService, loadtarget);
	}
	
	public static int getNotificationId() {
		return mId;
	}

    @TargetApi(20)
    private Notification.Action generateAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent( mPlayerService.getApplicationContext(), PlayerService.class );
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(mPlayerService.getApplicationContext(), 1, intent, 0);
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


        int pause;
        int play;
        int next;
        int smallIcon;

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

        String bitmapstring = icon == null ? "null" : "present";
        Log.d("NotificationPlayer", "Build notification, icon => " + bitmapstring);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mPlayerService)
                        .setSmallIcon(smallIcon)
                        .setContentTitle(item.title)
                        .setContentText(item.sub_title);

        if (mArtwork != null && !mArtwork.isRecycled()) {
            mBuilder.setLargeIcon(icon);
        }

        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        mBuilder.setCategory("CATEGORY_TRANSPORT");
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        mBuilder.setOnlyAlertOnce(true);

        Intent resultIntent = new Intent(mPlayerService, MainActivity.class);

        // Sets a custom content view for the notification, including an image button.
        RemoteViews layout = new RemoteViews(mPlayerService.getPackageName(), R.layout.notification);
        layout.setTextViewText(R.id.notification_title, item.title);
        layout.setTextViewText(R.id.notification_content, item.sub_title);
        layout.setImageViewBitmap(R.id.icon, icon);

        // Prepare intent which is triggered if the
        // notification is selected
        Intent toggleIntent = new Intent(NotificationReceiver.toggleAction);
        Intent nextIntent = new Intent(NotificationReceiver.nextAction);

        PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(mPlayerService, 0, toggleIntent, 0);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(mPlayerService, 0, nextIntent, 0);

        layout.setOnClickPendingIntent(R.id.play_pause_button,pendingToggleIntent);
        layout.setOnClickPendingIntent(R.id.next_button,pendingNextIntent);

        //PlayerStatusListener.registerImageView(, mPlayerService);

        int srcId = pause;
        if (isPlaying) srcId = play;

        layout.setImageViewResource(R.id.play_pause_button, srcId);
        layout.setImageViewResource(R.id.next_button, next);

        mBuilder.setContent(layout);


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

        mNotificationManager =
                (NotificationManager) mPlayerService.getSystemService(Context.NOTIFICATION_SERVICE);

        return mBuilder;
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
