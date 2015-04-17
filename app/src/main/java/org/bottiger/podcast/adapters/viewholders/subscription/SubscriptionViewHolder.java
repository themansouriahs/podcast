package org.bottiger.podcast.adapters.viewholders.subscription;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.bottiger.podcast.R;

/**
 * Created by apl on 15-04-2015.
 */
public class SubscriptionViewHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public SimpleDraweeView image;
    public View gradient;

    public SubscriptionViewHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.grid_title);
        image = (SimpleDraweeView) itemView.findViewById(R.id.grid_image);
        gradient = (View) itemView.findViewById(R.id.grid_item_gradient);

    }
}
