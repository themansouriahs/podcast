package org.bottiger.podcast.adapters;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.provider.FeedItem;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseIntArray;

import com.mobeta.android.dslv.DragSortListView;

public class ReorderCursor extends CursorWrapper implements DragSortListView.DropListener {
//class ReorderCursor extends CursorWrapper {

	PodcastBaseFragment mFragment = null;
	
    public ReorderCursor(Cursor cursor, PodcastBaseFragment fragment) {
        super(cursor);
        mFragment = fragment;
        _remapping = new SparseIntArray();
    }

    /**
     * This is the one being used as of 13 June 2013
     */
    @Override
    public void drop(final int from, final int to) {	
        // Update remapping
    	
        final int remapped_from = getRemappedPosition(from);
        if (from > to)
            for (int position = from; position > to; position--)
                _remapping.put(position, getRemappedPosition(position - 1));
        else // shift up
            for (int position = from; position < to; position++)
                _remapping.put(position, getRemappedPosition(position + 1)); 
        _remapping.put(to, remapped_from);
        
        FeedItem.clearCache();
         
         
        final CursorAdapter mAdapter = mFragment.getAdapter(this);
        final Context c = mFragment.getActivity();
		new Thread(new Runnable() {
			public void run() {
		
    	
        		if (from != to) {
					FeedItem precedingItem = null;
					if (to > 0) {
						Cursor precedingItemCursor = (Cursor) mAdapter
								.getItem(to - 1);
						precedingItem = FeedItem
								.getByCursor(precedingItemCursor);
					}

					Cursor item = (Cursor) mAdapter.getItem(to);
					FeedItem feedItem = FeedItem.getByCursor(item);

					feedItem.setPriority(precedingItem, c);
				}		
			}
		}).start();
		
		mAdapter.notifyDataSetChanged();
		
     }

    
    @Override
    public boolean moveToPosition(int position) {
    	int newPos = getRemappedPosition(position);
    	return super.moveToPosition(newPos);
    }

    private int getRemappedPosition(int position) {
        return _remapping.get(position, position);
    }

    private final SparseIntArray _remapping;
    
}