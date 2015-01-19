package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class AbstractPodcastAdapter extends RecyclerView.Adapter {

    protected Cursor mCursor;

    protected LayoutInflater mInflater;
    protected Context mContext;
    protected Activity mActivity;

    public AbstractPodcastAdapter(Cursor c) {
        mCursor = c;
	}

	public AbstractPodcastAdapter(Context context, int listItem, Cursor cursor,
			String[] strings, int[] is) {
        //super(context, listItem, cursor, strings, is);
        //super(context, cursor);
        mContext = context;
        mCursor = cursor;
	}

    public void setDataset(Cursor c) {
        mCursor = c;
    }

}
