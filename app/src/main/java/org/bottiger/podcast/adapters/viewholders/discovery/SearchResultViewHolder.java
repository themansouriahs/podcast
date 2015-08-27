package org.bottiger.podcast.adapters.viewholders.discovery;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.bottiger.podcast.R;

/**
 * Created by apl on 15-04-2015.
 */
public class SearchResultViewHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public SimpleDraweeView image;
    public android.support.v7.widget.SwitchCompat toggleSwitch;

    public Uri imageUrl = null;

    public SearchResultViewHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.result_title);
        image = (SimpleDraweeView) itemView.findViewById(R.id.result_image);
        toggleSwitch = (android.support.v7.widget.SwitchCompat) itemView.findViewById(R.id.result_subscribe_switch);

    }
}
