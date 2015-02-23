package org.bottiger.podcast.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Created by apl on 21-02-2015.
 */
public abstract class AbstractEpisodeCursorAdapter<T> extends RecyclerView.Adapter {

    protected Cursor mCursor;
    protected Context mContext;
    protected LayoutInflater mInflater;

    public AbstractEpisodeCursorAdapter(@NonNull Cursor argCursor) {
        mCursor = argCursor;
    }

    public void setDataset(@NonNull Cursor argCursor) {
        mCursor = argCursor;
    }
}
