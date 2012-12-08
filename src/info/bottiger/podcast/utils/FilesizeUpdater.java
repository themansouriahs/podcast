package info.bottiger.podcast.utils;

import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import info.bottiger.podcast.fetcher.FeedFetcher;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.service.PodcastDownloadManager;

public class FilesizeUpdater {

	private static HashMap<Long, TextView> viewTable = new HashMap<Long, TextView>();
	
	public static void put(Context context, long itemID, TextView tv) {
		FeedItem item = FeedItem.getById(context.getContentResolver(), itemID);
		viewTable.put(item.id, tv);
	}
	
	public static void put(FeedItem item, TextView tv) {
		viewTable.put(item.id, tv);
	}
	
	public static TextView get(FeedItem item) {
		TextView tv = viewTable.get(item.id);
		return tv;
	}
	
	private void updateTextView() {
		
		//new TextViewUpdater(this.item, this.tv).execute();
	}
	

	private class TextViewUpdater extends AsyncTask<Void, String, Void> {
		 TextView mTextView;
		 FeedItem mFeedItem;
		 
		 private long lastUpdate = 0;
		 
		 public TextViewUpdater(FeedItem item, TextView tv) {
			 this.mTextView = tv;
			 this.mFeedItem = item;
		 }
		 
	     protected Void doInBackground(Void... params) {
	    	 PodcastDownloadManager.DownloadStatus status = PodcastDownloadManager.getStatus(this.mFeedItem);
	    	 
	    	 while (status == PodcastDownloadManager.DownloadStatus.DOWNLOADING || 
	    			status == PodcastDownloadManager.DownloadStatus.PENDING) {
	    		 Long chunkFileSize = this.mFeedItem.chunkFilesize;
	    		 
	    		 try {
	    			 publishProgress(chunkFileSize.toString());
	    		 } catch(NullPointerException e) {
	    			 return null;
	    		 }
	    		 
	    		 try {
					Thread.sleep(1000);
	    		 } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
	    		 }
	    	 }
	    	 
	    	 return null;
	     }
	     
	     protected void onProgressUpdate(String... filesize) {
	    	 if (mFeedItem.isDownloaded()) {
	    		 long currentTime = System.currentTimeMillis();
	    		 if (currentTime > lastUpdate + 1000) {
	    			 this.mTextView.setText(filesize[0] + "MB");
	    			 lastUpdate = currentTime;
	    		 }
	    	 } else
	    		 this.mTextView.setText("waiting");
	     }
	 }
}
