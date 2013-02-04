package info.bottiger.podcast.listeners;

import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.service.DownloadStatus;
import info.bottiger.podcast.service.PodcastDownloadManager;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class DownloadProgressListener {

	/**
	 * How often the UI should refresh
	 */
	private static long REFRESH_INTERVAL = TimeUnit.SECONDS.convert(1,
			TimeUnit.MILLISECONDS);

	private static HashMap<TextView, TextViewProgress> mTextViews = new HashMap<TextView, TextViewProgress>();
	private static TextViewProgress mTextViewProgress;
	private static DownloadManager mDownloadManager = null;

	/**
	 * Handler events types
	 */
	private static final int REFRESH = 1;

	/**
	 * Handler for updating the textviews
	 */
	private static final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				for (TextViewProgress textViewProgress: mTextViews.values())
					textViewProgress.updateTextViewProgress();
				break;
			}
		}
	};

	/**
	 * Register a TextView to be updated on progress updates
	 * 
	 * @param textView
	 */
	public static void registerTextView(Context context, TextView textView, FeedItem item) {
		if (textView == null || item == null || context == null)
			return;
		
		if (mDownloadManager == null) {
			mDownloadManager = (DownloadManager) context
					.getSystemService(Context.DOWNLOAD_SERVICE);
		}

		mTextViewProgress = new TextViewProgress(textView, item);

		// If mTextViews doesn't contain any elements we should start the update
		// process
		if (mTextViews.size() == 0) {
			mHandler.removeMessages(REFRESH);
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.sendMessageDelayed(msg, REFRESH_INTERVAL);
		}

		mTextViews.put(textView, mTextViewProgress);
	}

	/**
	 * Unregister a TextView to be updated on progress updates
	 * 
	 * @param textView
	 */
	public static boolean unregisterTextView(TextView textView, FeedItem item) {
		mTextViewProgress = new TextViewProgress(textView, item);
		boolean isRemoved = false;
		
		if (mTextViews.containsKey(textView))
			isRemoved = mTextViews.remove(textView) != null;
		
		/*
		 * Stop the handler if the list is empty again
		 */
		if (mTextViews.size() == 0) {
			mHandler.removeMessages(REFRESH);
		}

		return isRemoved;
	}

	/**
	 * Private class containing the relation between a textView and an episode
	 * 
	 * @author Arvid Böttiger
	 * 
	 */
	private static class TextViewProgress {

		private TextView mTextView;
		private FeedItem mItem;

		public TextViewProgress(TextView mTextView, FeedItem item) {
			super();
			this.mTextView = mTextView;
			this.mItem = item;
		}

		/**
		 * Set the status of the textView for the FeedItem
		 * 
		 * @return whether the view was updated successfully  
		 */
		public boolean updateTextViewProgress() {
			mTextView.setText(getStatus());
			return false;
		}
		
		/**
		 * Writes the currentstatus of the FeedItem with the giving ID to the
		 * textView argument
		 * 
		 * @param itemID
		 * @param textView
		 * @param downloadStatus
		 */
		private String getStatus() {
			DownloadStatus downloadStatus = PodcastDownloadManager.getStatus(mItem);
			String statusText = "";
			switch (downloadStatus) {
			case PENDING:
				statusText = "waiting";
				break;
			case DOWNLOADING:
				statusText = getProgress();
				break;
			case DONE:
				statusText = "Done";
				break;
			case ERROR:
				statusText = "Error";
				break;
			default:
				statusText = "";
			}
			return statusText;
		}
		
		/**
		 * Get the current download progress as a String.
		 * 
		 * @return download status in percent
		 */
		private String getProgress() {
			int percent = 0;
			
			//FIXME This is run one time for each textview. It should only be run once with all the reference ID's
			Query query = new Query();
			query.setFilterById(mItem.getDownloadReferenceID());
			Cursor c = mDownloadManager.query(query);
			c.moveToFirst();
			while (c.isAfterLast() == false) 
			{
			    int cursorBytesSoFarIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
			    int cursorBytesTotalIndex =  c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
			    
			    int bytesSoFar = c.getInt(cursorBytesSoFarIndex);
			    int bytesTotal = c.getInt(cursorBytesTotalIndex);
			    
			    percent = bytesSoFar*100/bytesTotal;
			    
			    c.moveToNext();
			}
			
			return percent + "%";
		}

	}

}
