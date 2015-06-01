package org.bottiger.podcast.receiver;


import java.io.File;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.utils.SDCardManager;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/**
 * Receives event when a download is complete. If the file is succesfully
 * downloaded we move it to the currect destination.
 * 
 * @author bottiger
 * 
 */
public class DownloadManagerReceiver extends BroadcastReceiver {

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

				FeedItem item = FeedItem.getByDownloadReference(
						context.getContentResolver(), downloadId);

                /*
                HashMap<Long, List<DownloadObserver>> fileObserverHashMap = DownloadProgressObservable.getObservers();
                if (fileObserverHashMap.containsKey(item.getId())) {
                    List<DownloadObserver> observers = fileObserverHashMap.get(item.getId());
                    for (DownloadObserver downloadObserver : observers) {
                        downloadObserver.setProgressPercent(100);
                    }
                }*/

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
					EpisodeDownloadManager.notifyDownloadComplete(item);
					
					EpisodeDownloadManager.startDownload(context);
				}
			}

			EpisodeDownloadManager.removeExpiredDownloadedPodcasts(context);
		}
	}
	
	/**
	 * Update the FeedItem in teh SQLite database
	 * 
	 * @param c
	 * @param item
	 * @param downloadId
	 * @param context
	 */
	private void updateFeedItemIfSuccessful(Cursor c, FeedItem item,
			long downloadId, Context context) {

		String currentLocation = c.getString(c
				.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

		item.setDownloaded(true);
		item.filesize = c.getInt(c
				.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

		String filename =  Integer.toString(item.getEpisodeNumber()) + item.title.replace(' ', '_'); //Integer.toString(item.getEpisodeNumber()) + "_"
		item.setFilename(filename + ".mp3"); // .replaceAll("[^a-zA-Z0-9_-]", "") +

		/* Calculate the imagePath */
		String imageURL = null;
		if (item != null) {
			//imageURL = new BitmapProvider(context, item).getThumbnailPath();
			//item.image = imageURL; // FIXME
		}

		// Rename the file
		File oldFile = new File(currentLocation);
		File newFileName = new File(item.getAbsolutePath());
		oldFile.renameTo(newFileName);

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(newFileName));
        context.sendBroadcast(intent);

		if (EpisodeDownloadManager.getmDownloadingIDs().contains(downloadId))
			EpisodeDownloadManager.getmDownloadingIDs().remove(downloadId);

		/*
		 * If no more files are being downloaded we purge the tmp dir. Things
		 * might build up here if downloads are aborted for various reasons.
		 */
		if (EpisodeDownloadManager.getmDownloadingIDs().size() == 0) {
			File directory = new File(SDCardManager.getTmpDir());

			// Get all files in directory

			File[] files = directory.listFiles();
			for (File file : files) {
				// Delete each file

				if (!file.delete()) {
					// Failed to delete file

					System.out.println("Failed to delete " + file);
				}
			}
		}
	}
}
