package org.bottiger.podcast.adapters.viewholders.subscription;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.views.dialogs.DialogFeedAuthentication;

/**
 * Created by aplb on 31-12-2015.
 */
public class AuthenticationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public String url = null;

    public AuthenticationViewHolder(View itemView) {
        super(itemView);

        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (url == null)
            return;

        Activity host = (Activity) view.getContext();
        DialogFeedAuthentication dialogPlaybackSpeed = DialogFeedAuthentication.newInstance(url);
        dialogPlaybackSpeed.show(host.getFragmentManager(), DialogFeedAuthentication.class.getName());
    }
}
