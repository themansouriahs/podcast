package org.bottiger.podcast.adapters.viewholders.discovery;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.bottiger.podcast.R;

/**
 * Created by apl on 15-04-2015.
 */
public class SearchResultViewHolder extends RecyclerView.ViewHolder {

    public TextView title;

    public SearchResultViewHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.result_title);

    }
}
