package info.bottiger.podcast.notification;

import info.bottiger.podcast.NotificationReceiver;
import info.bottiger.podcast.R;
import info.bottiger.podcast.SwipeActivity;
import info.bottiger.podcast.provider.FeedItem;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class NotificationPlayer {
	
	private Context mContext;
	private FeedItem item;
	private int mId = 7;
	
	public NotificationPlayer(Context context) {
		super();
		this.mContext = context;
	}
	
	public void show() {
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(mContext)
		        .setSmallIcon(R.drawable.generic_podcast)
		        .setContentTitle("My notification")
		        .setContentText("Hello World!");
		
		Intent resultIntent = new Intent(mContext, NotificationReceiver.class);
		
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(SwipeActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		Notification not = mBuilder.build();
		mNotificationManager.notify(mId, not);
	}

}
