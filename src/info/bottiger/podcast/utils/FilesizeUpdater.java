package info.bottiger.podcast.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import info.bottiger.podcast.fetcher.FeedFetcher;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.service.PodcastDownloadManager;

public class FilesizeUpdater {

	private FeedItem item;
	private TextView tv;
	
	public FilesizeUpdater(FeedItem item, TextView tv) {
		this.item = item;
		this.tv = tv;
		this.updateTextView();
	}
	

	public FilesizeUpdater(Context context, long itemID, TextView tv) {
		this.item = FeedItem.getById(context.getContentResolver(), itemID);
		this.tv = tv;
		this.updateTextView();
	}
	
	private void updateTextView() {
		new TextViewUpdater(this.item, this.tv).execute();
	}
	

	private class TextViewUpdater extends AsyncTask<Void, String, Void> {
		 TextView mTextView;
		 FeedItem mFeedItem;
		 
		 public TextViewUpdater(FeedItem item, TextView tv) {
			 this.mTextView = tv;
			 this.mFeedItem = item;
		 }
		 
	     protected Void doInBackground(Void... params) {
	    	 PodcastDownloadManager.DownloadStatus status = PodcastDownloadManager.getStatus(this.mFeedItem);
	    	 
	    	 while (status == PodcastDownloadManager.DownloadStatus.DOWNLOADING) {
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
	    	 this.mTextView.setText(filesize[0] + "MB");
	     }
	 }
}
