package org.bottiger.podcast.notification;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PlayerStatusListener;
import org.bottiger.podcast.provider.BitmapProvider;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.receiver.NotificationReceiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

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
        layout.setOnClickPendingIntent(R.id.next_button,pendingNextIntent);
        
        //PlayerStatusListener.registerImageView(, mContext);
        
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
