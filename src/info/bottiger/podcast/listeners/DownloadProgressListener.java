package info.bottiger.podcast.listeners;

import info.bottiger.podcast.provider.FeedItem;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.app.DownloadManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class DownloadProgressListener {

	/**
	 * How often the UI should refresh
	 */
	private static long REFRESH_INTERVAL = TimeUnit.MILLISECONDS.convert(1,
			TimeUnit.SECONDS);

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
					refreshUI();
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
			refreshUI();
		}

		mTextViews.put(textView, mTextViewProgress);
	}

	/**
	 * Unregister a TextView to be updated on progress updates
	 * 
	 * @param textView
	 */
	public static boolean unregisterTextView(TextView textView, FeedItem item) {
		if (textView == null || item == null)
			return false;
		
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
	 * Refrersh the UI handler
	 */
	private static void refreshUI() {
		mHandler.removeMessages(REFRESH);
		Message msg = mHandler.obtainMessage(REFRESH);
		mHandler.sendMessageDelayed(msg, REFRESH_INTERVAL);
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
			assert mTextView != null;
			assert item != null;
			
			this.mTextView = mTextView;
			this.mItem = item;
		}

		/**
		 * Set the status of the textView for the FeedItem
		 * 
		 * @return whether the view was updated successfully  
		 */
		public boolean updateTextViewProgress() {
			mTextView.setText(mItem.getStatus(mDownloadManager));
			return false;
		}
		

	}

}
