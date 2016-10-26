package org.bottiger.podcast.receiver;


import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

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

	private static final String TAG = "DownloadManagerReceiver";

	private DownloadManager downloadManager;

	@Override
	public void onReceive(Context context, Intent intent) {
		downloadManager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);

		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long downloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);

			Query query = new Query();
			query.setFilterById(downloadId);
			Cursor c = downloadManager.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c
						.getColumnIndex(DownloadManager.COLUMN_STATUS);

				/*
				Deprecated code

				FeedItem item = FeedItem.getByDownloadReference(
						context.getContentResolver(), downloadId);

				if (item != null) {

					if (DownloadManager.STATUS_SUCCESSFUL == c
							.getInt(columnIndex)) {
						updateFeedItemIfSuccessful(c, item, downloadId, context);
						item.setDownloaded(true);
					} else {
						item.setDownloaded(false);
					}
					item.update(context.getContentResolver());
					
					// Start next download
					
					// Reset downloadingItem 
					//SoundWavesDownloadManager.notifyDownloadComplete(item);
					
					//SoundWavesDownloadManager.startDownload(context);
				}
				*/
			}

			StorageUtils.removeExpiredDownloadedPodcasts(context);
		}
	}
}
