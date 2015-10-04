package org.bottiger.podcast.activities.downloadmanager;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadItemViewHolder extends RecyclerView.ViewHolder {

    private ViewDataBinding binding;
    private IEpisode mEpisode;

    public DownloadItemViewHolder(View rowView) {
        super(rowView);
        binding = DataBindingUtil.bind(rowView);
    }

    public void setEpisode(IEpisode argEpisode) {
        this.mEpisode = argEpisode;
    }

    public ViewDataBinding getBinding() {
        return binding;
    }
}
