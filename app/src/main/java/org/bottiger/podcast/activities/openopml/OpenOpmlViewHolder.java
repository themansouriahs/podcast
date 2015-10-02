package org.bottiger.podcast.activities.openopml;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

/**
 * Created by aplb on 21-09-2015.
 */
public class OpenOpmlViewHolder extends RecyclerView.ViewHolder {
    private ViewDataBinding binding;

    private SlimSubscription mSubscription;
    private int mPosition;

    public OpenOpmlViewHolder(View rowView) {
        super(rowView);
        binding = DataBindingUtil.bind(rowView);
    }

    public ViewDataBinding getBinding() {
        return binding;
    }

    public SlimSubscription getSubscription() {
        return mSubscription;
    }

    public void setSubscription(SlimSubscription mSubscription) {
        this.mSubscription = mSubscription;
    }

    public void onClickRemoveMark(View view) {
        mSubscription.markForSubscription(false);
    }

    public void onClickMark(View view) {
        mSubscription.markForSubscription(true);
    }

}
