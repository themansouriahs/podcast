package org.bottiger.podcast.provider;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

public class BulkUpdater {
	
	private ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
	
	/**
	 * Add a operation to execute
	 * @return 
	 */
	public void addOperation(ContentProviderOperation op) {
		ops.add(op);
	}
	
	/**
	 * Commit batch update
	 */
	public void commit(ContentResolver contentResolver) {
		try {
			contentResolver.applyBatch(PodcastProvider.AUTHORITY, ops);
		} catch (RemoteException e) {
			// do s.th.
		} catch (OperationApplicationException e) {
			// do s.th.
		}
		ops.clear();
	}

}
