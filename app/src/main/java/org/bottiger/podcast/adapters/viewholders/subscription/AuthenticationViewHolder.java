package org.bottiger.podcast.adapters.viewholders.subscription;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.bignerdranch.android.multiselector.MultiSelector;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.dialogs.DialogFeedAuthentication;

/**
 * Created by aplb on 31-12-2015.
 */
public class AuthenticationViewHolder extends BaseAnimatedSelectableViewHolder implements View.OnClickListener {

    private View mContainer;
    public String url = null;

    public AuthenticationViewHolder(View itemView, MultiSelector argMultiSelector) {
        super(itemView, argMultiSelector);

        mContainer = itemView.findViewById(R.id.subscription_container);

        itemView.setOnClickListener(this);
    }

    @NonNull
    @Override
    View getContainerView() {
        return mContainer;
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
