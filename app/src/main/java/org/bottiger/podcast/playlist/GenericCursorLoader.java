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
import android.widget.Adapter;

import org.bottiger.podcast.adapters.AbstractPodcastAdapter;
import org.bottiger.podcast.service.PodcastDownloadManager;

public abstract class GenericCursorLoader {

	final Fragment mFragment;
    final AbstractPodcastAdapter mAdapter;
	
	public void cursorPostProsessing(Cursor cursoer)
    {};

	public GenericCursorLoader(Fragment fragment, AbstractPodcastAdapter adapter, Cursor cursor) {

        this.mFragment = fragment;
        this.mAdapter = adapter;
	}

    public ReorderCursor getReorderCursor(Cursor cursor) {
        return new ReorderCursor(mFragment.getActivity(), cursor);
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

		mFragment.getLoaderManager().restartLoader(id, mBundle, loaderCallback);
	}

	private LoaderManager.LoaderCallbacks<Cursor> loaderCallback = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

			Uri uri = bundle.getParcelable("uri");
			String[] projection = bundle.getStringArray("projection");

			String order = bundle.getString("order");
			String condition = bundle.getString("condition");

            CursorLoader loader = new CursorLoader(mFragment.getActivity(), uri, projection,
					condition, null, order);
            loader.setUpdateThrottle(2000);

            return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

			// https://github.com/bauerca/drag-sort-listview/issues/20
			// final ReorderCursor wrapped_cursor = new ReorderCursor(cursor,
			// PodcastBaseFragment.this);
			final Cursor wrapped_cursor = cursor; // getReorderCursor(cursor);

			// mAdapter = getAdapter(wrapped_cursor);
			mAdapter.setDataset(wrapped_cursor); //.changeCursor(wrapped_cursor);
            mAdapter.notifyDataSetChanged();

			// Update playlist
			cursorPostProsessing(wrapped_cursor);

		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// This is called when the last Cursor provided to onLoadFinished()
			// above is about to be closed. We need to make sure we are no
			// longer using it.
			mAdapter.setDataset(null); //.swapCursor(null);
		}
	};

}
