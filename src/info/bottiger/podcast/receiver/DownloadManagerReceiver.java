package info.bottiger.podcast.receiver;

import info.bottiger.podcast.provider.FeedItem;

import java.io.File;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

/**
 * Receives event when a download is complete. If the file is succesfully downloaded we move it to the currect destination.
 * 
 * @author bottiger
 *
 */
public class DownloadManagerReceiver extends BroadcastReceiver {
	
	private DownloadManager downloadManager;

	@Override
	public void onReceive(Context context, Intent intent) {
		downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		
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
				if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
					String currentLocation = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
					
					FeedItem item = FeedItem.getByDownloadReference(context.getContentResolver(), downloadId);
					
					item.filesize = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

					String filename = Integer.toString(item.getEpisodeNumber()) + "_" + item.title.replace(' ', '_');
					item.setFilename(filename);
					
					// Rename the file
					File oldFile = new File(currentLocation);
					File newFileName = new File(item.getAbsolutePath());
					oldFile.renameTo(newFileName);
					
					item.update(context.getContentResolver());
				}
			}
		}
	}
}
