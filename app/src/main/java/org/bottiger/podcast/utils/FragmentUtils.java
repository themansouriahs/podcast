package org.bottiger.podcast.utils;

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


	/*
	 * This is a huge hack. FIXME
	 */
	public CursorAdapter getAdapter(Cursor cursor) {
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

}
