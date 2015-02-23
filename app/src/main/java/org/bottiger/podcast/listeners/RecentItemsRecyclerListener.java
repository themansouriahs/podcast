package org.bottiger.podcast.listeners;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Created by apl on 15-09-2014.
 */
public class RecentItemsRecyclerListener implements RecyclerView.RecyclerListener {

    private RecyclerView.Adapter mAdapter;

    public RecentItemsRecyclerListener(@NonNull RecyclerView.Adapter argAdapter) {
        mAdapter = argAdapter;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        Log.d("onViewRecycled", "View has been recycled: pos:" + viewHolder.getPosition());
        mAdapter.onViewRecycled(viewHolder);
    }
}
