package org.bottiger.podcast.activities.discovery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.adapters.viewholders.discovery.SearchResultViewHolder;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.featured.FeaturedPodcastsUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by apl on 15-04-2015.
 */
public class DiscoverySearchAdapter extends RecyclerView.Adapter<SearchResultViewHolder> {

    private static final String TAG = DiscoverySearchAdapter.class.getSimpleName();

    private Activity mActivity;
    private LayoutInflater mInflater;

    private int mDefaultBackgroundColor = 0;
    private ArrayList<ISubscription> mDataset = new ArrayList<>();
    private HashSet<URL> mSubscribedUrls = new HashSet<>();

    public DiscoverySearchAdapter(@NonNull Activity argActivity) {
        mActivity = argActivity;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultBackgroundColor = ContextCompat.getColor(mActivity, R.color.colorPrimary);

        populateSubscribedUrls();
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");
        View view = mInflater.inflate(R.layout.discovery_item, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, final int position) {
        final ISubscription subscription = mDataset.get(position);

        if (FeaturedPodcastsUtil.isFeatured(subscription)) {
            holder.title.setText(FeaturedPodcastsUtil.getFeaturedHeadline(subscription, mActivity.getResources()));
        } else {
            holder.title.setText(subscription.getTitle());
        }

        holder.getDescription().setText(subscription.getDescription());

        try {

            String imageUrl = subscription.getImageURL();
            if (StrUtils.isValidUrl(imageUrl)) {
                RequestOptions options = ImageLoaderUtils.getRequestOptions(mActivity, ImageLoaderUtils.NETWORK);
                options = options.centerCrop();
                RequestBuilder<Bitmap> builder = ImageLoaderUtils.getGlide(mActivity, imageUrl, options);
                builder.into(new BitmapImageViewTarget(holder.image) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        holder.image.setImageBitmap(resource);
                    }
                });
            }
        } catch (NullPointerException npe) {
            holder.image.setBackgroundColor(mDefaultBackgroundColor);
        }

        final URL url = subscription.getURL();

        holder.itemView.setOnClickListener(v -> {

            boolean isLocal = SoundWaves.getAppContext(mActivity).getLibraryInstance().containsSubscription(subscription);
            ISubscription openSubscription = isLocal ?
                    SoundWaves.getAppContext(mActivity).getLibraryInstance().getSubscription(subscription.getURLString()) :
                    subscription;

            assert openSubscription != null;
            FeedActivity.start(mActivity, openSubscription);
        });

        boolean isSubscribed = mSubscribedUrls.contains(url);

        holder.toggleSwitch.setOnCheckedChangeListener(null);
        holder.toggleSwitch.setChecked(isSubscribed);
        holder.toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleSubscriptionStatus(holder.itemView, subscription));
    }

    @Override
    public void onViewRecycled(SearchResultViewHolder holder) {
        clearCache(holder);
        holder.toggleSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    public void onViewDetachedFromWindow(SearchResultViewHolder holder) {
        clearCache(holder);
        super.onViewDetachedFromWindow(holder);
    }

    private synchronized void clearCache(SearchResultViewHolder holder) {
        if (holder.imageUrl != null) {
            holder.imageUrl = null;
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public long getItemId(int argPosition) {
        final ISubscription subscription = mDataset.get(argPosition);
        return subscription.getURL().toString().hashCode(); // FIXME
    }

    public void setDataset(@NonNull ArrayList<ISubscription> argDataset) {
        mDataset = argDataset;
        notifyDataSetChanged();
    }

    public void populateSubscribedUrls() {
        List<Subscription> subscriptionSortedList = SoundWaves.getAppContext(mActivity).getLibraryInstance().getLiveSubscriptions().getValue();

        mSubscribedUrls.clear();

        Subscription subscription;
        for (int i = 0; i < subscriptionSortedList.size(); i++) {
            subscription = subscriptionSortedList.get(i);
            if (subscription.getStatus() == Subscription.STATUS_SUBSCRIBED) {
                mSubscribedUrls.add(subscription.getURL());
            }
        }
    }

    private synchronized void toggleSubscriptionStatus(@NonNull View argView,
                                                      @NonNull ISubscription argSubscription) {
        final URL url = argSubscription.getURL();
        final boolean isSubscribed = mSubscribedUrls.contains(url);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final Subscription subscription = new Subscription(sharedPreferences, url.toString());
        if (isSubscribed) {
            unsubscribe(subscription);
            mSubscribedUrls.remove(url);
        } else {
            subscribe(subscription);
            mSubscribedUrls.add(url);
        }

        UIUtils.displaySubscribedSnackbar(isSubscribed, argSubscription, argView, mActivity);
    }

    private void subscribe(@NonNull ISubscription argSubscription) {
        SoundWaves.getAppContext(mActivity).getLibraryInstance().subscribe(argSubscription);
    }

    private void unsubscribe(@NonNull Subscription argSubscription) {
        SoundWaves.getAppContext(mActivity).getLibraryInstance().unsubscribe(argSubscription.getURLString(), "DiscoveryAdapter:unsubscribe");
    }
}
