package org.bottiger.podcast.utils;

import org.bottiger.podcast.SubscriptionsFragment;
import org.bottiger.podcast.PodcastBaseFragment.OnItemSelectedListener;
import org.bottiger.podcast.provider.FeedItem;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseIntArray;
import android.view.View;

import com.mobeta.android.dslv.DragSortListView;

public class FragmentUtils {
	
	private Activity mActivity;
	private Fragment mFragment;
	private View mView;
	
	public FragmentUtils(Activity activity, View view, Fragment fragment) {
		super();
		assert activity != null;
		this.mActivity = activity;
		this.mFragment = fragment;
		this.mView = view;
	}

	private CursorAdapter mAdapter;
	private OnItemSelectedListener mListener;
	private Cursor mCursor;

	protected final PodcastLog log = PodcastLog.getLog(getClass());
	
	public void startInit(int id, Uri columns, String[] projection,
			String condition, String order) {
		assert condition != null;
		assert order != null;
		assert projection != null;

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		Bundle mBundle = new Bundle();
		mBundle.putParcelable("uri", columns);
		mBundle.putStringArray("projection", projection);
		mBundle.putString("order", order);
		mBundle.putString("condition", condition);
		
		// FIXME
		mFragment.getLoaderManager().restartLoader(id, mBundle, loaderCallback);
		// //getLoaderManager().initLoader(id, mBundle, loaderCallback);
	}
	
	/*
	 * This is a huge hack. FIXME
	 */
	public CursorAdapter getAdapter(Cursor cursor) {
		/*
		if (mFragment instanceof SubscriptionsFragment) {
			SubscriptionsFragment sf = (SubscriptionsFragment) mFragment;
			mAdapter = sf.getAdapter();
		}
		*/
		return mAdapter;
	}
	
	public CursorAdapter getAdapter() {
		return mAdapter;
	}

	public void setAdapter(CursorAdapter mAdapter) {
		this.mAdapter = mAdapter;
	}

	public OnItemSelectedListener getListener() {
		return mListener;
	}

	public void setListener(OnItemSelectedListener mListener) {
		this.mListener = mListener;
	}

	public Cursor getCursor() {
		return mCursor;
	}

	public void setCursor(Cursor mCursor) {
		this.mCursor = mCursor;
	}
	
	public Fragment getFragment() {
		return mFragment;
	}

	public void setFragment(Fragment fragment) {
		this.mFragment = fragment;
	}
	
	private LoaderManager.LoaderCallbacks<Cursor> loaderCallback = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

			Uri uri = bundle.getParcelable("uri");
			String[] projection = bundle.getStringArray("projection");

			String order = bundle.getString("order");
			String condition = bundle.getString("condition");

			return new CursorLoader(mActivity, uri, projection, condition,
					null, order);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			
			// https://github.com/bauerca/drag-sort-listview/issues/20
			final ReorderCursor wrapped_cursor = new ReorderCursor(cursor);
			
			// FIXME - should not be commented out
			mAdapter = getAdapter(wrapped_cursor);
			mAdapter.changeCursor(wrapped_cursor);

			/*
            //((CursorAdapter) getListView().getAdapter()).swapCursor(wrapped_cursor);
			View currentListView = mView;
			if (currentListView instanceof DragSortListView) {
				((DragSortListView) currentListView).setDropListener(wrapped_cursor);
			}
			
			// The list should now be shown.
			if (mFragment.isResumed()) {
				//setListShown(true); //FIXME
			} else {
				if (mFragment instanceof FixedListFragment)
					((FixedListFragment)mFragment).setListShownNoAnimation(true);
			}
			*/
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// This is called when the last Cursor provided to onLoadFinished()
			// above is about to be closed. We need to make sure we are no
			// longer using it.
			mAdapter.swapCursor(null);
			
			// https://github.com/bauerca/drag-sort-listview/issues/20
			//((CursorAdapter) getListView().getAdapter()).swap(null);

		}
	};
	
	class ReorderCursor extends CursorWrapper implements DragSortListView.DropListener {

	    ReorderCursor(Cursor cursor) {
	        super(cursor);
	        _remapping = new SparseIntArray();
	    }

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

						Context c = FragmentUtils.this.mActivity;
						feedItem.setPriority(precedingItem, c);
					}		
				}
			}).start();
					
			mAdapter.notifyDataSetChanged();
			
	     }

	    @Override
	    public boolean moveToPosition(int position) {
	    	return super.moveToPosition(getRemappedPosition(position));
	    }

	    private int getRemappedPosition(int position) {
	        return _remapping.get(position, position);
	    }

	    private final SparseIntArray _remapping;
	}

}
