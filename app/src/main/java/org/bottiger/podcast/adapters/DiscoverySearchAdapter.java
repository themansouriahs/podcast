package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
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

    private Activity mActivity;
    private LayoutInflater mInflater;

    private int mDefaultBackgroundColor = 0;
    private ArrayList<ISubscription> mDataset = new ArrayList<>();

    public DiscoverySearchAdapter(@NonNull Activity argActivity) {
        mActivity = argActivity;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultBackgroundColor = mActivity.getResources().getColor(R.color.colorPrimary);
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

        try {
            Uri  uri = Uri.parse(subscription.getImageURL());
            holder.image.setImageURI(uri);
        } catch (NullPointerException npe) {
            holder.image.setBackgroundColor(mDefaultBackgroundColor);
        }
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
