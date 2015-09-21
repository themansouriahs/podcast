package org.bottiger.podcast.activities.openopml;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 21-09-2015.
 */
public class OpenOpmlAdapter extends RecyclerView.Adapter<OpenOpmlViewHolder> {

    private List<SlimSubscription> mSlimSubscriptions = new LinkedList<>();

    public OpenOpmlAdapter(List<SlimSubscription> argSubscriptions) {
        this.mSlimSubscriptions = argSubscriptions;
    }

    @Override
    public OpenOpmlViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_opml_import_item, parent, false);
        OpenOpmlViewHolder holder = new OpenOpmlViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(OpenOpmlViewHolder holder, int position) {
        final SlimSubscription subscription = mSlimSubscriptions.get(position);
        holder.getBinding().setVariable(BR.subscription, subscription);
        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mSlimSubscriptions.size();
    }
}
