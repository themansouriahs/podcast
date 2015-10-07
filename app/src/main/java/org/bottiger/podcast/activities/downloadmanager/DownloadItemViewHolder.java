package org.bottiger.podcast.activities.downloadmanager;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.views.ImageViewTinted;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadItemViewHolder extends RecyclerView.ViewHolder {

    private ViewDataBinding binding;
    private IEpisode mEpisode;

    @Deprecated
    public ImageViewTinted mImageView;

    public DownloadItemViewHolder(View rowView) {
        super(rowView);
        binding = DataBindingUtil.bind(rowView);
        mImageView = (ImageViewTinted) rowView.findViewById(R.id.download_episode_image);
    }

    public void setEpisode(IEpisode argEpisode) {
        this.mEpisode = argEpisode;
    }

    public ViewDataBinding getBinding() {
        return binding;
    }
}
