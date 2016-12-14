package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.ToolbarActivity;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.adapters.viewholders.subscription.AuthenticationViewHolder;
import org.bottiger.podcast.adapters.viewholders.subscription.SubscriptionViewHolder;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.StrUtils;

import java.util.List;

/**
 * Created by aplb on 11-10-2015.
 */
public class SubscriptionAdapter extends RecyclerView.Adapter {

    private static final String TAG = "SubscriptionAdapter";

    private static final int GRID_TYPE = 1;
    private static final int LIST_TYPE = 2;
    private static final int FOOTER_TYPE = 3;
    private static final int AUTHENTICATE_TYPE = 4;

    private final LayoutInflater mInflater;
    private Activity mActivity;

    @Nullable
    private SortedList<Subscription> mSubscriptions = null;

    private int numberOfColumns = 2;
    private int position = -1;

    @Nullable private ActionMode mActionMode = null;
    private MultiSelector mMultiSelector = new MultiSelector();
    private ModalMultiSelectorCallback mActionModeCallback = getModalMultiSelectorCallback();

    public SubscriptionAdapter(Activity argActivity, int argColumnsCount) {
        mActivity = argActivity;
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        numberOfColumns = argColumnsCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        RecyclerView.ViewHolder holder;

        switch (viewType)
        {
            case AUTHENTICATE_TYPE: {
                View view = mInflater.inflate(R.layout.subscription_authenticate, parent, false);
                holder = new AuthenticationViewHolder(view, mMultiSelector);
                break;
            }
            default: {
                View view = mInflater.inflate(getGridItemLayout(), parent, false);
                holder = new SubscriptionViewHolder(view, mMultiSelector);
                break;
            }
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder argHolder, final int position) {

        Subscription subscription = null;
        try {
            subscription = mSubscriptions.get(position);
        } catch (IllegalStateException e) {
            ErrorUtils.handleException(e);
        }

        if (null == subscription) {
            return;
        }

        switch (getItemViewType(position)) {
            case AUTHENTICATE_TYPE: {
                onBindAuthenticationHolder((AuthenticationViewHolder) argHolder, subscription);
            }
            case GRID_TYPE:
            case LIST_TYPE:{
                onBindDefaultHolder((SubscriptionViewHolder)argHolder, subscription);
            }
        }
    }

    private void onBindAuthenticationHolder(AuthenticationViewHolder argHolder, @NonNull final Subscription argSubscription) {
        argHolder.url = argSubscription.getURLString();
    }

    private void onBindDefaultHolder(@NonNull final SubscriptionViewHolder argHolder, @NonNull final Subscription argSubscription) {

        final String logo = argSubscription.getImageURL();
        final Resources resources = mActivity.getResources();

        int newEpisodeCount = argSubscription.getNewEpisodes();
        CharSequence pluralNew = getNewEpisodesString(newEpisodeCount);
        boolean hasNewEpisodes = newEpisodeCount > 0;
        boolean hasImage = !TextUtils.isEmpty(logo);
        int visibility = hasNewEpisodes || !hasImage ? View.VISIBLE : View.GONE;
        Uri imageURI = hasImage ? Uri.parse(logo) : null;
        int generic_image_margin = 100;
        int position = argHolder.getAdapterPosition();

        if (argSubscription.getLastItemUpdated() > 0 && argHolder.subTitle != null) {
            String reportDate = DateUtils.getRelativeTimeSpanString(argSubscription.getLastItemUpdated()).toString();
            String updatedAt = resources.getString(R.string.subscription_subtitle_updated_at);
            argHolder.subTitle.setText(String.format("%s %s", updatedAt, reportDate));
        }

        if (argHolder.new_episodes_counter != null && argHolder.new_episodes != null) {

            if (hasNewEpisodes) {
                argHolder.new_episodes_counter.setText(String.valueOf(newEpisodeCount));
                argHolder.new_episodes.setText(pluralNew);
            }

            argHolder.new_episodes_counter.setVisibility(visibility);
            argHolder.new_episodes.setVisibility(visibility);
        } else {
            String title = String.valueOf(newEpisodeCount) + " " + pluralNew;
            if (hasNewEpisodes && !isListView() && hasImage) {
                argHolder.title.setText(title);
            }
        }

        if (argHolder.text_container != null) {
            if (argHolder.image != null && hasImage) {

                argSubscription.getColors(mActivity)
                        .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                        .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {

                            @Override
                            public void onSuccess(ColorExtractor value) {
                                argHolder.text_container.setBackgroundColor(value.getPrimaryTint());
                            }
                        });
            } else {
                argHolder.text_container.setBackgroundColor(0x00000000);
            }
        }

        if (hasImage) {
            argHolder.image.setPadding(0, 0, 0, 0);
            String image = imageURI.toString();
            if (!TextUtils.isEmpty(image) && StrUtils.isValidUrl(image)) {

                if (getItemViewType(position) == GRID_TYPE) {
                    ImageLoaderUtils.getGlide(mActivity, image).centerCrop().placeholder(R.drawable.generic_podcast).into(argHolder.image);
                } else {
                    ImageLoaderUtils.getGlide(mActivity, image)
                            .centerCrop()
                            .placeholder(R.drawable.generic_podcast)
                            .into(new BitmapImageViewTarget(argHolder.image) {
                                @Override
                                protected void setResource(Bitmap resource) {
                                    RoundedBitmapDrawable circularBitmapDrawable =
                                            RoundedBitmapDrawableFactory.create(mActivity.getResources(), resource);
                                    float radius = mActivity.getResources().getDimension(R.dimen.playlist_image_radius_small);

                                    circularBitmapDrawable.setCornerRadius(radius);

                                    argHolder.image.setImageDrawable(circularBitmapDrawable);
                                }
                            });
                }

            }
        } else {
            argHolder.image.setPadding(generic_image_margin, generic_image_margin, generic_image_margin, generic_image_margin);
            Glide.with(mActivity).load(R.drawable.generic_podcast).centerCrop().into(argHolder.image);
        }

        if (argHolder.text_container != null) {
            argHolder.text_container.setVisibility(visibility);
        }

        if (isListView() || !hasImage) {
            argHolder.title.setText(argSubscription.getTitle());
        }


        argHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMultiSelector.tapSelection(argHolder)){
                    // do whatever we want to do when not in selection mode
                    // perhaps navigate to a detail screen
                    FeedActivity.start(mActivity, argSubscription);
                } else {
                    onSubscriptionSelected();
                }
            }
        });

        argHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                AppCompatActivity appCompatActivity = ((AppCompatActivity) argHolder.itemView.getContext());
                mActionMode = appCompatActivity.startSupportActionMode(mActionModeCallback);

                if (!mMultiSelector.isSelectable()) {
                    mMultiSelector.setSelectable(true);
                    mMultiSelector.setSelected(argHolder, true);
                    return true;
                }

                mMultiSelector.tapSelection(argHolder);
                onSubscriptionSelected();

                return true;

            }
        });
    }

    private void onSubscriptionSelected() {
        int numSelected = mMultiSelector.getSelectedPositions().size();
        setActionModeTitle(numSelected);
        if (numSelected < 1 && mActionMode != null) {
            mActionMode.finish();
        }
    }

    public void setDataset(@NonNull SortedList<Subscription> argSubscriptions) {
        mSubscriptions = argSubscriptions;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        Subscription sub;
        try {
            sub = mSubscriptions.get(position);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return FOOTER_TYPE;
        }

        if (sub != null && !sub.isAuthenticationWorking()) {
            return AUTHENTICATE_TYPE;
        }

        return numberOfColumns == 1 ? LIST_TYPE : GRID_TYPE;
    }

    private boolean isListView() {
        return numberOfColumns == 1;
    }

    private int getGridItemLayout() {
        return isListView() ? R.layout.subscription_list_item : R.layout.subscription_grid_item ;
    }

    public void setNumberOfColumns(int argNumber) {
        if (numberOfColumns != argNumber) {
            numberOfColumns = argNumber;
            notifyDataSetChanged();
        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int getItemCount() {
        if (mSubscriptions == null)
            return 0;

        return mSubscriptions.size();
    }

    private boolean setActionModeTitle(int argNumSelectedItems) {
        ActionMode actionMode = mActionMode;

        if (actionMode == null) {
            return false;
        }

        if (argNumSelectedItems > 0) {
            Resources res = mActivity.getResources();
            String formattedString = res.getQuantityString(R.plurals.subscriptions_selected, argNumSelectedItems, argNumSelectedItems);
            actionMode.setTitle(formattedString);
            return true;
        }

        return false;
    }

    private ModalMultiSelectorCallback getModalMultiSelectorCallback() {
        return new ModalMultiSelectorCallback(mMultiSelector) {
            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

                List<Integer> positions = mMultiSelector.getSelectedPositions();

                if (mSubscriptions == null)
                    return false;

                switch (menuItem.getItemId()) {
                    case R.id.unsubscribe:
                        actionMode.finish();

                        for (int i = 0; i < positions.size(); i++) {
                            int position = positions.get(i);
                            Subscription subscription = mSubscriptions.get(position);

                            if (subscription == null)
                                return false;

                            subscription.unsubscribe("Unsubscribe:context");
                            notifyItemRemoved(position);
                        }

                        mMultiSelector.clearSelections();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                super.onCreateActionMode(actionMode, menu);
                mActivity.getMenuInflater().inflate(R.menu.subscription_context, menu);

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
        };
    }

    private CharSequence getNewEpisodesString(int newEpisodeCount) {
        CharSequence pluralNew = "";
        if (newEpisodeCount > 0) {
            pluralNew = mActivity.getResources().getQuantityText(R.plurals.subscription_list_new, newEpisodeCount);
        }

        return pluralNew;
    }
}
