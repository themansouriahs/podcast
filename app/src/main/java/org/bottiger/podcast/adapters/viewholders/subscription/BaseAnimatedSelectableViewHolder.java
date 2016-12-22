package org.bottiger.podcast.adapters.viewholders.subscription;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.MultiSelectorBindingHolder;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 14-12-2016.
 */

abstract class BaseAnimatedSelectableViewHolder extends MultiSelectorBindingHolder implements ISubscriptionViewHolder {

    private static final String TAG = BaseAnimatedSelectableViewHolder.class.getSimpleName();
    private static final float DEFAULT_SCALE = 0.8f;

    private boolean mSelectable = false;
    private boolean mActivated = false;
    private boolean mIsPinned = false;

    @Nullable
    private TextView mImagePlaceholderText;

    @NonNull private ImageView mCheckMark;
    @NonNull private ImageView mIsPinnedMark;

    BaseAnimatedSelectableViewHolder(View itemView, MultiSelector multiSelector) {
        super(itemView, multiSelector);
        mCheckMark = (ImageView) itemView.findViewById(R.id.subscription_selected_mark);
        mIsPinnedMark = (ImageView) itemView.findViewById(R.id.subscription_is_pinned);
        mImagePlaceholderText = (TextView) itemView.findViewById(R.id.grid_image_placeholder);
    }

    @NonNull
    abstract View getContainerView();

    public void setIsPinned(boolean argIsPinned) {
        mIsPinned = argIsPinned;
        int visibility = mIsPinned ? View.VISIBLE : View.GONE;
        mIsPinnedMark.setVisibility(visibility);
    }

    @Override
    public void setSelectable(boolean argIsSelectable) {
        if (mSelectable == argIsSelectable) {
            return;
        }
        mSelectable = argIsSelectable;
        if (!argIsSelectable) {
            changeLayoutState(false);
        }
    }

    @Override
    public boolean isSelectable() {
        return mSelectable;
    }

    @Override
    public void setActivated(boolean isActivated) {
        if (mActivated == isActivated) {
            return;
        }
        mActivated = isActivated;
        changeLayoutState(mActivated);
    }

    @Override
    public boolean isActivated() {
        return mActivated;
    }

    private void changeLayoutState(boolean isSelected) {
        View view = getContainerView();
        Log.d(TAG, view.toString() + " => " + isSelected);
        float scale = isSelected ? DEFAULT_SCALE : 1.0f;
        int checkMarkVisibility = isSelected ? View.VISIBLE : View.GONE;
        int pinnedVisibility = !isSelected && mIsPinned ? View.VISIBLE : View.GONE;
        view.animate().scaleX(scale).scaleY(scale).start();
        mCheckMark.setVisibility(checkMarkVisibility);
        mIsPinnedMark.setVisibility(pinnedVisibility);
    }

    public void setImagePlaceholderText(@NonNull String argTitle) {
        if (mImagePlaceholderText == null)
            return;

        mImagePlaceholderText.setText(argTitle);
    }

    public void setImagePlaceholderVisibility(int argVisibility) {
        if (mImagePlaceholderText == null)
            return;

        mImagePlaceholderText.setVisibility(argVisibility);
    }
}
