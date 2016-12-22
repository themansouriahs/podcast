package org.bottiger.podcast.activities.discovery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.adapters.viewholders.discovery.SearchResultViewHolder;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.SharedAdapterUtils;
import org.bottiger.podcast.utils.UIUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by apl on 15-04-2015.
 */
public class DiscoverySearchAdapter extends RecyclerView.Adapter<SearchResultViewHolder> {

    private static final String TAG = "DiscoverySearchAdapter";

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

        holder.title.setText(subscription.getTitle());

        try {
            ImageLoaderUtils.loadImageInto(holder.image, subscription.getImageURL(), null, false, true, true, ImageLoaderUtils.NETWORK);
        } catch (NullPointerException npe) {
            holder.image.setBackgroundColor(mDefaultBackgroundColor);
        }

        final URL url = subscription.getURL();

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean isLocal = SoundWaves.getAppContext(mActivity).getLibraryInstance().containsSubscription(subscription);
                ISubscription openSubscription = isLocal ?
                        SoundWaves.getAppContext(mActivity).getLibraryInstance().getSubscription(subscription.getURLString()) :
                        subscription;

                assert openSubscription != null;
                FeedActivity.start(mActivity, openSubscription);
            }
        });

        boolean isSubscribed = mSubscribedUrls.contains(url);

        holder.toggleSwitch.setOnCheckedChangeListener(null);
        holder.toggleSwitch.setChecked(isSubscribed);
        holder.toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleSubscriptionStatus(holder.itemView, subscription);
            }
        });
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
        SortedList<Subscription> subscriptionSortedList = SoundWaves.getAppContext(mActivity).getLibraryInstance().getSubscriptions();

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
