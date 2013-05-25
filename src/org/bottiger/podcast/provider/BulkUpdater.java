package org.bottiger.podcast.provider;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

public class BulkUpdater {
	
	private ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
	private ArrayList<ContentProviderOperation> subscriptionOps = new ArrayList<ContentProviderOperation>();
	private ArrayList<ContentProviderOperation> episodeOps = new ArrayList<ContentProviderOperation>();
	
	/**
	 * Add a operation to execute
	 * @return 
	 */
	public void addOperation(ContentProviderOperation op) {
		ops.add(op);
		/*
		if (op.getUri().equals(ItemColumns.URI))
			episodeOps.add(op);
		else if (op.getUri().equals(SubscriptionColumns.URI))
			subscriptionOps.add(op);
		 */
	}
	
	/**
	 * Commit batch update
	 */
	public void commit(ContentResolver contentResolver) {
		try {
			contentResolver.applyBatch(PodcastProvider.AUTHORITY, ops);
			/*
			contentResolver.applyBatch(PodcastProvider.AUTHORITY, subscriptionOps);
			contentResolver.applyBatch(PodcastProvider.AUTHORITY, episodeOps);
			*/
		} catch (RemoteException e) {
			// do s.th.
		} catch (OperationApplicationException e) {
			// do s.th.
		}
		ops.clear();
		/*
		subscriptionOps.clear();
		episodeOps.clear();
		*/
	}

}
