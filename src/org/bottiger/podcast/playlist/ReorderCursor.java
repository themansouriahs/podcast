package org.bottiger.podcast.playlist;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.provider.FeedItem;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseIntArray;

import com.mobeta.android.dslv.DragSortListView;

public class ReorderCursor extends CursorWrapper implements
		DragSortListView.DropListener {

	private PodcastBaseFragment mBaseFragment = null;
	private CursorAdapter mAdapter = null;

	private final SparseIntArray _remapping;

	@Deprecated
	public ReorderCursor(Cursor cursor, PodcastBaseFragment fragment) {
		super(cursor);
		mBaseFragment = fragment;
		_remapping = new SparseIntArray();
	}

	public ReorderCursor(CursorAdapter adapter, Cursor cursor) {
		super(cursor);
		mAdapter = adapter;
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
		else
			// shift up
			for (int position = from; position < to; position++)
				_remapping.put(position, getRemappedPosition(position + 1));
		_remapping.put(to, remapped_from);

		FeedItem.clearCache();

		final CursorAdapter adapter = (mAdapter != null) ? mAdapter
				: mBaseFragment.getAdapter(this);
		final Context c = mBaseFragment.getActivity();
		
		asyncDatabaseUpdate(c, adapter, from , to);

		adapter.notifyDataSetChanged();

	}

	@Override
	public boolean moveToPosition(int position) {
		int newPos = getRemappedPosition(position);
		return super.moveToPosition(newPos);
	}

	private int getRemappedPosition(int position) {
		return _remapping.get(position, position);
	}

	private void asyncDatabaseUpdate(final Context context,
			final CursorAdapter adapter, final int from, final int to) {
		new Thread(new Runnable() {
			public void run() {

				if (from != to) {
					FeedItem precedingItem = null;
					if (to > 0) {
						Cursor precedingItemCursor = (Cursor) adapter
								.getItem(to - 1);
						precedingItem = FeedItem
								.getByCursor(precedingItemCursor);
					}

					Cursor item = (Cursor) adapter.getItem(to);
					FeedItem feedItem = FeedItem.getByCursor(item);

					feedItem.setPriority(precedingItem, context);
				}
			}
		}).start();
	}

}