package org.bottiger.podcast.adapters.viewholders.subscription;

import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.MultiSelectorBindingHolder;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.ImageViewTinted;

/**
 * Created by apl on 15-04-2015.
 */
public class SubscriptionViewHolder extends BaseAnimatedSelectableViewHolder {

    public View container; // the type of this view depends on if it's a list or a grid
    public TextView title;
    public TextView subTitle;
    public ImageViewTinted image;
    public FrameLayout text_container;

    public TextView new_episodes_counter;
    public TextView new_episodes;

    public SubscriptionViewHolder(View itemView, MultiSelector argMultiSelector) {
        super(itemView, argMultiSelector);

        container = itemView.findViewById(R.id.subscription_container);
        title = (TextView) itemView.findViewById(R.id.grid_title);
        subTitle = (TextView) itemView.findViewById(R.id.grid_subtitle);
        image = (ImageViewTinted) itemView.findViewById(R.id.grid_image);
        text_container = (FrameLayout)itemView.findViewById(R.id.subscription_text_container);

        new_episodes_counter = (TextView) itemView.findViewById(R.id.new_episodes_counter);
        new_episodes = (TextView) itemView.findViewById(R.id.new_episodes);
    }

    @NonNull
    @Override
    View getContainerView() {
        return container;
    }
}
