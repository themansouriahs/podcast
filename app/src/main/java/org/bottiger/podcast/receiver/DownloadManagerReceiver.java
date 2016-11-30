package org.bottiger.podcast.receiver;


import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.notification.NotificationPlayer;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.SDCardManager;
import org.bottiger.podcast.utils.StorageUtils;

import java.io.File;
import java.io.IOException;

/**
 * Receives event when a download is complete. If the file is succesfully
 * downloaded we move it to the currect destination.
 * 
 * @author bottiger
 * 
 */
public class DownloadManagerReceiver extends BroadcastReceiver {

	private static final String TAG = DownloadManagerReceiver.class.getSimpleName();

	public static final String fetchNewAction = ApplicationConfiguration.packageName + ".FETCH_NEW";

	private DownloadManager downloadManager;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "event received");

		downloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);

		String action = intent.getAction();
		if (fetchNewAction.equals(action)) {

		}
	}

	public static Intent getFetchNewIntent(@NonNull Context argContext) {
		String pkg = argContext.getPackageName();
		Intent fetchNewIntent = new Intent(fetchNewAction).setPackage(pkg);
		return fetchNewIntent;
	}
}
