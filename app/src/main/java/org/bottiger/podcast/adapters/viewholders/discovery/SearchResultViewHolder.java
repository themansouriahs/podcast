package org.bottiger.podcast.adapters.viewholders.discovery;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.ImageViewTinted;

/**
 * Created by apl on 15-04-2015.
 */
public class SearchResultViewHolder extends RecyclerView.ViewHolder {

    public View container;
    public TextView title;
    public ImageViewTinted image;
    public android.support.v7.widget.SwitchCompat toggleSwitch;

    public Uri imageUrl = null;

    public SearchResultViewHolder(View itemView) {
        super(itemView);

        container = itemView.findViewById(R.id.container);
        title = (TextView) itemView.findViewById(R.id.result_title);
        image = (ImageViewTinted) itemView.findViewById(R.id.result_image);
        toggleSwitch = (android.support.v7.widget.SwitchCompat) itemView.findViewById(R.id.result_subscribe_switch);

    }
}
