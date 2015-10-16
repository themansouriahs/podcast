package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.ToolbarActivity;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.adapters.viewholders.FooterViewHolder;
import org.bottiger.podcast.adapters.viewholders.subscription.SubscriptionViewHolder;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.model.SubscriptionChanged;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.rxbus.RxBusSimpleEvents;

import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by aplb on 11-10-2015.
 */
public class SubscriptionAdapter extends RecyclerView.Adapter {

    private static final String TAG = "SubscriptionAdapter";

    private static final int GRID_TYPE = 1;
    private static final int LIST_TYPE = 2;
    private static final int FOOTER_TYPE = 3;

    private final LayoutInflater mInflater;
    private Activity mActivity;
    private Library mLibrary;

    private int numberOfColumns = 2;
    private int position = -1;

    public SubscriptionAdapter(Activity argActivity, Library argLibrary, int argColumnsCount) {
        mActivity = argActivity;
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        numberOfColumns = argColumnsCount;
        mLibrary = argLibrary;

        SoundWaves.getRxBus().toObserverable()
                .ofType(SubscriptionChanged.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SubscriptionChanged>() {
                    @Override
                    public void call(SubscriptionChanged event) {
                        //if (event instanceof SubscriptionChanged) {
                        SubscriptionChanged subscriptionChanged = (SubscriptionChanged) event;
                        if (subscriptionChanged.getAction() == SubscriptionChanged.ADDED) {
                            notifyDataSetChanged();
                        }

                        if (subscriptionChanged.getAction() == SubscriptionChanged.REMOVED) {
                            notifyDataSetChanged();
                        }
                        //}
                    }
                });
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
            default: {
                View view = mInflater.inflate(getGridItemLayout(), parent, false);
                holder = new SubscriptionViewHolder(view);
                break;
            }
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder argHolder, int position) {

        // In case we are dealing with the footer
        if (isLastItem(position)) {
            FooterViewHolder footer = (FooterViewHolder)argHolder;
            //footer.getFooter().getLayoutParams().height = ToolbarActivity.getNavigationBarHeight(mActivity.getResources());

            android.support.v7.widget.GridLayoutManager.LayoutParams params = (android.support.v7.widget.GridLayoutManager.LayoutParams)footer.getFooter().getLayoutParams();
            params.height = ToolbarActivity.getNavigationBarHeight(mActivity.getResources());
            //params.columnSpec = GridLayout.spec(0, numberOfColumns-1);
            //params.s
            footer.getFooter().setLayoutParams(params);
            footer.getFooter().requestLayout();

            return;
        }

        final SubscriptionViewHolder holder = (SubscriptionViewHolder)argHolder;

        Subscription sub = null;
        try {
            //sub = SubscriptionLoader.getByCursor(cursor);
            sub = mLibrary.getSubscriptions().get(position);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if (null == sub) {
            return;
        }

        final Subscription subscription = sub;

        String title = sub.getTitle();
        final String logo = sub.getImageURL();

        if (title != null && !title.equals(""))
            holder.title.setText(title);
        else
            holder.title.setText(R.string.subscription_no_title);

        if (subscription.getLastUpdate() > 0 && holder.subTitle != null) {
            String reportDate = DateUtils.getRelativeTimeSpanString(subscription.getLastItemUpdated()).toString(); //df.format(date);

            String updatedAt = mActivity.getResources().getString(R.string.subscription_subtitle_updated_at);
            holder.subTitle.setText(updatedAt + " " + reportDate);
        }


        Uri uri = null;
        if (holder.image != null && !TextUtils.isEmpty(logo)) {
            uri = Uri.parse(logo);

            PaletteHelper.generate(logo, mActivity, new PaletteListener() {
                @Override
                public void onPaletteFound(Palette argChangedPalette) {
                    ColorExtractor extractor = new ColorExtractor(argChangedPalette);
                    holder.text_container.setBackgroundColor(extractor.getPrimary());
                    holder.title.setTextColor(extractor.getTextColor());
                }

                @Override
                public String getPaletteUrl() {
                    return logo;
                }
            });

        }

        if (uri != null) {
            String image = uri.toString();
            if (!TextUtils.isEmpty(image) && Patterns.WEB_URL.matcher(image).matches()) {

                //FrescoHelper.PalettePostProcessor postProcessor = new FrescoHelper.PalettePostProcessor(mActivity, image);
                //FrescoHelper.loadImageInto(holder.image, image, postProcessor);


                Glide.with(mActivity).load(image).centerCrop().placeholder(R.drawable.generic_podcast).into(holder.image);

                /*
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
                */

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
                setPosition((int)subscription.getId());
                return false;
            }
        });
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

        return numberOfColumns == 1 ? LIST_TYPE : GRID_TYPE;
    }

    private boolean isLastItem(int argPosition) {
        return argPosition +1 == getItemCount();
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
        int count = mLibrary.getSubscriptions().size();

        if (count == 0) // If there are 0 subscriptions we do not want to return 1
            return count;

        return count +1; // one footer please
    }

}
