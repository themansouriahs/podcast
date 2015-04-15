package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.viewholders.discovery.SearchResultViewHolder;
import org.bottiger.podcast.provider.ISubscription;

import java.util.ArrayList;

/**
 * Created by apl on 15-04-2015.
 */
public class DiscoverySearchAdapter extends RecyclerView.Adapter<SearchResultViewHolder> {

    private static final String TAG = "DiscoverySearchAdapter";

    private LayoutInflater mInflater;

    private ArrayList<ISubscription> mDataset = new ArrayList<>();

    public DiscoverySearchAdapter(@NonNull Activity argActivity) {
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        View view = mInflater.inflate(R.layout.discovery_item, parent, false);
        SearchResultViewHolder holder = new SearchResultViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, final int position) {
        ISubscription subscription = mDataset.get(position);
        holder.title.setText(subscription.getTitle());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setDataset(@NonNull ArrayList<ISubscription> argDataset) {
        mDataset = argDataset;
        notifyDataSetChanged();
    }
}
