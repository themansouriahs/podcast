package org.bottiger.podcast.playlist;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;

import com.mobeta.android.dslv.DragSortListView;

public class GenericCursorLoader {

	final CursorAdapter mAdapter;
	final Fragment mFragment;

	public GenericCursorLoader(Fragment fragment, CursorAdapter adapter) {
		this.mFragment = fragment;
		this.mAdapter = adapter;
	}

	protected void loadCursor(int id, Uri columns, String[] projection,
			String condition, String order) {
		assert condition != null;
		assert order != null;
		assert projection != null;

		/*
		if (id != 10)
			setListAdapter(mAdapter);
			*/

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		Bundle mBundle = new Bundle();
		mBundle.putParcelable("uri", columns);
		mBundle.putStringArray("projection", projection);
		mBundle.putString("order", order);
		mBundle.putString("condition", condition);

		// FIXME
		mFragment.getLoaderManager().restartLoader(id, mBundle, loaderCallback);
		// getLoaderManager().initLoader(id, mBundle, loaderCallback);
	}

	private LoaderManager.LoaderCallbacks<Cursor> loaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

			Uri uri = bundle.getParcelable("uri");
			String[] projection = bundle.getStringArray("projection");

			String order = bundle.getString("order");
			String condition = bundle.getString("condition");

			return new CursorLoader(mFragment.getActivity(), uri, projection,
					condition, null, order);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

			// https://github.com/bauerca/drag-sort-listview/issues/20
			// final ReorderCursor wrapped_cursor = new ReorderCursor(cursor,
			// PodcastBaseFragment.this);
			final ReorderCursor wrapped_cursor = new ReorderCursor(mAdapter,
					cursor);

			// mAdapter = getAdapter(wrapped_cursor);
			mAdapter.changeCursor(wrapped_cursor);

			// ((CursorAdapter)
			// getListView().getAdapter()).swapCursor(wrapped_cursor);
			View fragmentView = mFragment.getView();

			View currentView = fragmentView.findViewById(android.R.id.list);
			if (currentView instanceof DragSortListView) {
				DragSortListView mDslv = (DragSortListView) currentView;
				if (mDslv != null) {
					mDslv.setDropListener(wrapped_cursor);
				}
			}

			// The list should now be shown.
			/*
			 * if (isResumed()) { // setListShown(true); //FIXME } else { //
			 * setListShownNoAnimation(true); //FIXME as well }
			 */
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// This is called when the last Cursor provided to onLoadFinished()
			// above is about to be closed. We need to make sure we are no
			// longer using it.
			mAdapter.swapCursor(null);
		}
	};

}
