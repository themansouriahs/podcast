package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.graphics.Palette;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.ToolbarActivity;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.adapters.viewholders.FooterViewHolder;
import org.bottiger.podcast.adapters.viewholders.subscription.AuthenticationViewHolder;
import org.bottiger.podcast.adapters.viewholders.subscription.SubscriptionViewHolder;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.StrUtils;

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

    public SubscriptionAdapter(Activity argActivity, int argColumnsCount) {
        mActivity = argActivity;
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        numberOfColumns = argColumnsCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        RecyclerView.ViewHolder holder = null;

        switch (viewType)
        {
            case FOOTER_TYPE: {
                View view = mInflater.inflate(R.layout.recycler_item_empty_footer, parent, false);
                holder = new FooterViewHolder(view);
                break;
            }
            case AUTHENTICATE_TYPE: {
                View view = mInflater.inflate(R.layout.subscription_authenticate, parent, false);
                holder = new AuthenticationViewHolder(view);
                break;
            }
            default: {
                View view = mInflater.inflate(getGridItemLayout(), parent, false);
                holder = new SubscriptionViewHolder(view);
                break;
            }
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder argHolder, final int position) {

        // In case we are dealing with the footer
        if (isLastItem(position)) {
            FooterViewHolder footer = (FooterViewHolder)argHolder;

            android.support.v7.widget.GridLayoutManager.LayoutParams params = (android.support.v7.widget.GridLayoutManager.LayoutParams)footer.getFooter().getLayoutParams();
            params.height = ToolbarActivity.getNavigationBarHeight(mActivity.getResources());
            footer.getFooter().setLayoutParams(params);
            footer.getFooter().requestLayout();

            return;
        }

        Subscription sub = null;
        try {
            sub = mSubscriptions.get(position);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if (null == sub) {
            return;
        }

        if (getItemViewType(position) == AUTHENTICATE_TYPE) {
            AuthenticationViewHolder holder = (AuthenticationViewHolder)argHolder;
            holder.url = sub.getURLString();
            return;
        }

        final SubscriptionViewHolder holder = (SubscriptionViewHolder)argHolder;

        final Subscription subscription = sub;


        final String logo = sub.getImageURL();

        if (subscription.getLastItemUpdated() > 0 && holder.subTitle != null) {
            String reportDate = DateUtils.getRelativeTimeSpanString(subscription.getLastItemUpdated()).toString(); //df.format(date);

            String updatedAt = mActivity.getResources().getString(R.string.subscription_subtitle_updated_at);
            holder.subTitle.setText(String.format("%s %s", updatedAt, reportDate));
        }


        int newEpisodeCount = subscription.getNewEpisodes();
        boolean hasNewEpisodes = newEpisodeCount > 0;
        CharSequence pluralNew = "";
        if (hasNewEpisodes) {
            pluralNew = mActivity.getResources().getQuantityText(R.plurals.subscription_list_new, newEpisodeCount);
        }
        int visibility = hasNewEpisodes ? View.VISIBLE : View.GONE;

        if (holder.new_episodes_counter != null && holder.new_episodes != null) {

            if (hasNewEpisodes) {
                holder.new_episodes_counter.setText(String.valueOf(newEpisodeCount));
                holder.new_episodes.setText(pluralNew);
            }

            holder.new_episodes_counter.setVisibility(visibility);
            holder.new_episodes.setVisibility(visibility);
        } else {
            String title = String.valueOf(newEpisodeCount) + " " + pluralNew;
            if (hasNewEpisodes && !isListView()) {
                holder.title.setText(title);
            }
        }

        if (isListView()) {
            holder.title.setText(subscription.getTitle());
        }

        if (holder.text_container != null) {
            holder.text_container.setVisibility(visibility);
        }


        Uri uri = null;
        if (holder.image != null && !TextUtils.isEmpty(logo)) {
            uri = Uri.parse(logo);

            PaletteHelper.generate(logo, mActivity, new PaletteListener() {
                @Override
                public void onPaletteFound(Palette argChangedPalette) {
                    ColorExtractor extractor = new ColorExtractor(argChangedPalette);
                    if (holder.text_container != null)
                        holder.text_container.setBackgroundColor(extractor.getPrimaryTint());
                }

                @Override
                public String getPaletteUrl() {
                    return logo;
                }
            });

        }

        if (uri != null) {
            String image = uri.toString();
            if (!TextUtils.isEmpty(image) && StrUtils.isValidUrl(image)) {

                if (getItemViewType(position) == GRID_TYPE) {
                    Glide.with(mActivity).load(image).centerCrop().placeholder(R.drawable.generic_podcast).into(holder.image);
                } else {
                    Glide.with(mActivity)
                            .load(image)
                            .asBitmap()
                            .centerCrop()
                            .placeholder(R.drawable.generic_podcast)
                            .into(new BitmapImageViewTarget(holder.image) {
                                @Override
                                protected void setResource(Bitmap resource) {
                                    RoundedBitmapDrawable circularBitmapDrawable =
                                            RoundedBitmapDrawableFactory.create(mActivity.getResources(), resource);
                                    float radius = mActivity.getResources().getDimension(R.dimen.playlist_image_radius_small);

                                    circularBitmapDrawable.setCornerRadius(radius);

                                    holder.image.setImageDrawable(circularBitmapDrawable);
                                }
                            });
                }

            }
        } else {
            Glide.with(mActivity).load(R.drawable.generic_podcast).centerCrop().into(holder.image);
        }

        argHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedActivity.start(mActivity, subscription);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // This saves the position in the adapter.
                // The unsubscribing is done in the fragment
                setPosition((int)subscription.getId());
                return false;
            }
        });
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
        if (isLastItem(position))
            return FOOTER_TYPE;

        Subscription sub = null;
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

    private boolean isLastItem(int argPosition) {
        return false; //argPosition +1 == getItemCount();
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
}
