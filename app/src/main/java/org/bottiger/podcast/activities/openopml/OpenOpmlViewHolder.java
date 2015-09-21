package org.bottiger.podcast.activities.openopml;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by aplb on 21-09-2015.
 */
public class OpenOpmlViewHolder extends RecyclerView.ViewHolder {
    private ViewDataBinding binding;

    public OpenOpmlViewHolder(View rowView) {
        super(rowView);
        binding = DataBindingUtil.bind(rowView);
    }

    public ViewDataBinding getBinding() {
        return binding;
    }
}
