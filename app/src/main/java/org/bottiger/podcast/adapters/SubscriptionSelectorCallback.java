package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.util.SortedList;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ColorUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 14-12-2016.
 */

class SubscriptionSelectorCallback extends ModalMultiSelectorCallback {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NONE, ALL, SOME})
    public @interface PinState {}
    public static final int NONE = 1;
    public static final int ALL = 2;
    public static final int SOME = 3;

    @NonNull private Activity mActivity;
    @NonNull private MultiSelector mMultiSelector;
    @NonNull private SubscriptionAdapter mAdapter;

    @PinState private int mPinState;

    SubscriptionSelectorCallback(@NonNull Activity argActivity,
                                        @NonNull SubscriptionAdapter argAdapter,
                                        @NonNull MultiSelector argMultiSelector) {
        super(argMultiSelector);
        mActivity = argActivity;
        mMultiSelector = argMultiSelector;
        mAdapter = argAdapter;
        mPinState = NONE;
    }

    void setPinState(@PinState int argState) {
        mPinState = argState;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

        List<Integer> positions = mMultiSelector.getSelectedPositions();
        SortedList<Subscription> subscriptions = mAdapter.getDataset();

        if (subscriptions == null)
            return false;

        switch (menuItem.getItemId()) {
            case R.id.unsubscribe:
                actionMode.finish();

                List<Subscription> toBeRemoved = new LinkedList<>();
                for (int i = 0; i < positions.size(); i++) {
                    int position = positions.get(i);
                    Subscription subscription = subscriptions.get(position);

                    if (subscription == null)
                        return false;

                    toBeRemoved.add(subscription);
                }

                for (int i = 0; i < toBeRemoved.size(); i++) {
                    Subscription subscription = toBeRemoved.get(i);
                    subscription.unsubscribe("Unsubscribe:context");
                }

                if (toBeRemoved.size() > 0) {
                    mAdapter.notifyDataSetChanged();
                }

                mMultiSelector.clearSelections();
                return true;
            case R.id.menu_star_subscription: {
                actionMode.finish();

                boolean doPin = mPinState != ALL;

                // Run backwards to ensure that the positions will be correct
                boolean changed = false;
                mAdapter.getDataset().beginBatchedUpdates();
                for (int i = 0; i < positions.size(); i++) {
                    int position = positions.get(i);
                    Subscription subscription = subscriptions.get(position);

                    if (subscription == null)
                        return false;

                    subscription.setIsPinned(doPin);
                    changed = true;
                    //mAdapter.notifyItemMoved(position, 0);
                    //mAdapter.pinSubscription(subscription, position, doPin);
                }
                mAdapter.getDataset().endBatchedUpdates();

                if (changed) {
                    SoundWaves.getAppContext(mActivity).getLibraryInstance().resetOrder();
                    mAdapter.notifyDataSetChanged();
                }

                mMultiSelector.clearSelections();
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        super.onCreateActionMode(actionMode, menu);
        mActivity.getMenuInflater().inflate(R.menu.subscription_context, menu);

        Drawable drawable = menu.findItem(R.id.menu_star_subscription).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        @ColorInt int textColor = ColorUtils.getBackgroundColor(mActivity);
        DrawableCompat.setTint(drawable, textColor);
        menu.findItem(R.id.menu_star_subscription).setIcon(drawable);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean onPrepared = super.onPrepareActionMode(mode, menu);

        String title = String.valueOf(mMultiSelector.getSelectedPositions().size() + 1 );
        mode.setTitle(title);

        return onPrepared;
    }

    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
    }
}
