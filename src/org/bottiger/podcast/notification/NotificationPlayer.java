package org.bottiger.podcast.notification;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.NotificationReceiver;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.BitmapProvider;
import org.bottiger.podcast.provider.FeedItem;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class NotificationPlayer {
	
	private Context mContext;
	private FeedItem item;
	
	private NotificationManager mNotificationManager = null;
	private int mId = 7;
	
	public NotificationPlayer(Context context, FeedItem item) {
		super();
		this.mContext = context;
		this.item = item;
	}
	
	public Notification show() {
		
		Bitmap icon = new BitmapProvider(mContext, item).createBitmapFromMediaFile(128, 128);
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(mContext)
		        .setSmallIcon(R.drawable.soundwaves)
		        .setContentTitle(item.title)
		        .setContentText(item.sub_title)
		        .setLargeIcon(icon);
		
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
		mNotificationManager =
		    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		Notification not = mBuilder.build();
		mNotificationManager.notify(mId, not);
		return not;
	}
	
	public void hide() {
		mNotificationManager.cancel(mId);
	}

	public FeedItem getItem() {
		return item;
	}

	public void setItem(FeedItem item) {
		this.item = item;
	}
	
	public int getNotificationId() {
		return mId;
	}

	
}
