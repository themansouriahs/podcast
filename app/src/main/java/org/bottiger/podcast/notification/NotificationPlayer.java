package org.bottiger.podcast.notification;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.images.FrescoHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.receiver.NotificationReceiver;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.DirectExecutor;

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

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

public class NotificationPlayer {
	
	private PlayerService mPlayerService;
	private IEpisode item;

    private Notification mNotification;
	private NotificationManager mNotificationManager = null;

	private static final int mId = 4260;
	
	public NotificationPlayer(@NonNull PlayerService argPlayerService, @NonNull IEpisode item) {
		super();
		this.mPlayerService = argPlayerService;
		this.item = item;
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
        NotificationCompat.Builder builder = buildNotification(isPlaying, mPlayerService, null);

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

    public void refresh(@Nullable Bitmap argBitmap) {
        PlayerService ps = mPlayerService;
        if (ps != null) {
            NotificationCompat.Builder notificationBuilder = buildNotification(ps.isPlaying(), mPlayerService, argBitmap);

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

	public IEpisode getItem() {
		return item;
	}

	public void setItem(IEpisode item) {
		this.item = item;
        refresh(null);
	}
	
	public static int getNotificationId() {
		return mId;
	}

    @Nullable
    private NotificationCompat.Builder buildNotification(@NonNull Boolean isPlaying, @NonNull PlayerService argPlayerService, @Nullable Bitmap argBitmap) {

        if (item == null) {
            return null;
        }


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

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mPlayerService)
                        .setSmallIcon(smallIcon)
                        .setContentTitle(item.getTitle());
                        //.setContentText(item.sub_title);

        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        mBuilder.setCategory("CATEGORY_TRANSPORT");
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        mBuilder.setOnlyAlertOnce(true);

        Intent resultIntent = new Intent(mPlayerService, MainActivity.class);

        // Sets a custom content view for the notification, including an image button.
        RemoteViews layout = new RemoteViews(mPlayerService.getPackageName(), R.layout.notification);
        layout.setTextViewText(R.id.notification_title, item.getTitle());
        //layout.setTextViewText(R.id.notification_content, item.sub_title);

        if (argBitmap != null && !argBitmap.isRecycled()) {
            mBuilder.setLargeIcon(argBitmap);
            layout.setImageViewBitmap(R.id.icon, argBitmap);
        } else {

            final String imageUrl = item.getArtwork(mPlayerService);

            FrescoHelper.fetchBitmap(new FrescoHelper.IBitmapFetchJob() {
                @NonNull
                @Override
                public Context getContext() {
                    return mPlayerService.getApplicationContext();
                }

                @NonNull
                @Override
                public String getUrl() {
                    return imageUrl;
                }

                @Override
                public void onSucces(@Nullable Bitmap argBitmap) {
                    if (argBitmap != null && !argBitmap.isRecycled())
                        refresh(argBitmap);
                }

                @Override
                public void onFail(@Nullable DataSource argDataSource) {

                }
            });

        }

        // Prepare intent which is triggered if the
        // notification is selected
        Intent toggleIntent = new Intent(NotificationReceiver.toggleAction);
        Intent nextIntent = new Intent(NotificationReceiver.nextAction);
        Intent clearIntent = new Intent(NotificationReceiver.clearAction);

        PendingIntent pendingToggleIntent = PendingIntent.getBroadcast(mPlayerService, 0, toggleIntent, 0);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(mPlayerService, 0, nextIntent, 0);
        PendingIntent pendingClearIntent = PendingIntent.getBroadcast(mPlayerService, 0, clearIntent, 0);

        layout.setOnClickPendingIntent(R.id.play_pause_button,pendingToggleIntent);
        layout.setOnClickPendingIntent(R.id.next_button,pendingNextIntent);
        layout.setOnClickPendingIntent(R.id.clear_button,pendingClearIntent);

        //PlayerStatusListener.registerImageView(, mPlayerService);

        int srcId = pause;
        if (isPlaying) srcId = play;

        layout.setImageViewResource(R.id.play_pause_button, srcId);
        layout.setImageViewResource(R.id.next_button, next);
        layout.setImageViewResource(R.id.clear_button, clear);

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
