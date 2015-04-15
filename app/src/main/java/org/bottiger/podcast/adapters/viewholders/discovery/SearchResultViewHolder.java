package org.bottiger.podcast.adapters.viewholders.discovery;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.bottiger.podcast.R;

/**
 * Created by apl on 15-04-2015.
 */
public class SearchResultViewHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public SimpleDraweeView image;

    public SearchResultViewHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.result_title);
        image = (SimpleDraweeView) itemView.findViewById(R.id.result_image);

    }
}
