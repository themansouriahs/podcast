package org.bottiger.podcast.adapters.viewholders;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 16-09-2015.
 */
public class FooterViewHolder extends RecyclerView.ViewHolder {

    private View mFooter;

    public FooterViewHolder(View itemView) {
        super(itemView);

        mFooter = itemView.findViewById(R.id.recycler_footer);
    }

    @NonNull
    public View getFooter() {
        return mFooter;
    }
}
