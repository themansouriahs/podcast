package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;

import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.viewholders.discovery.SearchResultViewHolder;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.SharedAdapterUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

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
    private LruCache<String,SlimSubscription> mCache = new LruCache<>(10);

    private static ProgressDialog mProgress;

    IDownloadCompleteCallback mRefreshCompleteCallback = new IDownloadCompleteCallback() {
        @Override
        public void complete(boolean argSucces, ISubscription argSubscription) {
            mProgress.dismiss();

            if (!argSucces)
                return;

            // FIXME why do I get Subscriptions here?
            if (argSubscription instanceof Subscription)
                return;

            SlimSubscription slimSubscription = (SlimSubscription)argSubscription;
            FeedActivity.startSlim(mActivity, slimSubscription.getURLString(), slimSubscription);
        }
    };

    public DiscoverySearchAdapter(@NonNull Activity argActivity) {
        mActivity = argActivity;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultBackgroundColor = mActivity.getResources().getColor(R.color.colorPrimary);

        mProgress = new ProgressDialog(mActivity);

        populateSubscribedUrls();
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        View view = mInflater.inflate(R.layout.discovery_item, parent, false);
        SearchResultViewHolder holder = new SearchResultViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, final int position) {
        final ISubscription subscription = mDataset.get(position);

        holder.title.setText(subscription.getTitle());

        try {
            Uri  uri = Uri.parse(subscription.getImageURL());
            holder.image.setImageURI(uri);
            holder.imageUrl = uri;
        } catch (NullPointerException npe) {
            holder.image.setBackgroundColor(mDefaultBackgroundColor);
        }

        SharedAdapterUtils.AddPaddingToLastElement(holder.container, 0, position == mDataset.size()-1);

        final URL url = subscription.getURL();

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mProgress.setTitle("Loading");
                mProgress.setMessage(mActivity.getString(R.string.discovery_progress_loading_podcast_content));
                mProgress.show();
                SoundWaves.sSubscriptionRefreshManager.refresh(subscription, mRefreshCompleteCallback);
            }
        });

        boolean isSubscribed = mSubscribedUrls.contains(url);

        holder.toggleSwitch.setOnCheckedChangeListener(null);
        holder.toggleSwitch.setChecked(isSubscribed);
        holder.toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleSubscriptionStatus(subscription);
            }
        });

        //SoundWaves.sSubscriptionRefreshManager.refresh(subscription, mRefreshCompleteCallback);
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
            Fresco.getImagePipeline().evictFromMemoryCache(holder.imageUrl);
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

    private void populateSubscribedUrls() {
        ContentResolver contentResolver = mActivity.getContentResolver();
        LinkedList<Subscription> subscriptions = SubscriptionLoader.allAsList(contentResolver);

        for (Subscription subscription : subscriptions) {
            if (subscription.getStatus() == Subscription.STATUS_SUBSCRIBED) {
                mSubscribedUrls.add(subscription.getURL());
            }
        }
    }

    public synchronized void toggleSubscriptionStatus(@NonNull ISubscription argSubscription) {
        URL url = argSubscription.getURL();
        boolean isSubscribed = mSubscribedUrls.contains(url);
        Subscription subscription = new Subscription(url.toString());
        if (isSubscribed) {
            unsubscribe(subscription);
            mSubscribedUrls.remove(url);
        } else {
            subscribe(subscription);
            mSubscribedUrls.add(url);
        }

        // remember tjat isSubscribed is inverted now
        int stringId = !isSubscribed ? R.string.discovery_subscribe_toast : R.string.discovery_unsubscribe_toast;
        String text = mActivity.getResources().getString(stringId);
        String formattedText = String.format(text, argSubscription.getTitle());

        Context context = mActivity.getApplicationContext();
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, formattedText, duration);
        toast.show();
    }

    private void subscribe(@NonNull Subscription argSubscription) {
        argSubscription.subscribe(mActivity);
    }

    private void unsubscribe(@NonNull Subscription argSubscription) {
        argSubscription.unsubscribe(mActivity);
    }
}
