package org.bottiger.podcast.adapters.viewholders.subscription;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.ImageViewTinted;

/**
 * Created by apl on 15-04-2015.
 */
public class SubscriptionViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

    public View container; // the type of this view depends on if it's a list or a grid
    public TextView title;
    public TextView subTitle;
    public ImageViewTinted image;
    public View gradient;

    public SubscriptionViewHolder(View itemView) {
        super(itemView);

        container = itemView.findViewById(R.id.subscription_container);
        title = (TextView) itemView.findViewById(R.id.grid_title);
        subTitle = (TextView) itemView.findViewById(R.id.grid_subtitle);
        image = (ImageViewTinted) itemView.findViewById(R.id.grid_image);
        gradient = (View) itemView.findViewById(R.id.grid_item_gradient);

        itemView.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //menu.setHeaderTitle("Select The Action");
    }
}
