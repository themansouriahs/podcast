package org.bottiger.podcast.playlist;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import org.bottiger.podcast.adapters.FeedViewAdapter;
import org.bottiger.podcast.adapters.SubscriptionGridCursorAdapter;

public abstract class GenericSubscriptionLoader {

    final LoaderManager mSupportLoaderManager;
    final Activity mActivity;
    final SubscriptionGridCursorAdapter mAdapter;

	public GenericSubscriptionLoader(Fragment fragment, SubscriptionGridCursorAdapter adapter, Cursor cursor) {
        this.mSupportLoaderManager = fragment.getLoaderManager();
        this.mActivity = fragment.getActivity();
        this.mAdapter = adapter;
	}

    protected void loadCursor(int id, Uri columns, String[] projection,
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

        mSupportLoaderManager.restartLoader(id, mBundle, loaderCallbackSupport);
	}

	private LoaderCallbacks<Cursor> loaderCallbackSupport = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

			Uri uri = bundle.getParcelable("uri");
			String[] projection = bundle.getStringArray("projection");

			String order = bundle.getString("order");
			String condition = bundle.getString("condition");

            CursorLoader loader = new CursorLoader(mActivity, uri, projection,
					condition, null, order);
            //loader.setUpdateThrottle(2000);

            return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

			// https://github.com/bauerca/drag-sort-listview/issues/20
			// final ReorderCursor wrapped_cursor = new ReorderCursor(cursor,
			// PodcastBaseFragment.this);
			final Cursor wrapped_cursor = cursor; // getReorderCursor(cursor);

			mAdapter.changeCursor(wrapped_cursor); //.setDataset(wrapped_cursor); //.changeCursor(wrapped_cursor);
            mAdapter.notifyDataSetChanged();

		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// This is called when the last Cursor provided to onLoadFinished()
			// above is about to be closed. We need to make sure we are no
			// longer using it.
			mAdapter.changeCursor(null); //.swapCursor(null);
		}
	};

}
