package org.bottiger.podcast;

import org.bottiger.podcast.adapters.FeedViewAdapter;
import org.bottiger.podcast.images.PicassoWrapper;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.playlist.FeedCursorLoader;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.DownloadCompleteCallback;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.BackgroundTransformation;
import org.bottiger.podcast.utils.FragmentUtils;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.views.FeedRecyclerView;
import org.bottiger.podcast.views.FeedRefreshLayout;
import org.bottiger.podcast.views.RelativeLayoutWithBackground;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


public class FeedFragment extends AbstractEpisodeFragment implements PaletteListener, FeedRefreshLayout.OnRefreshListener, DownloadCompleteCallback {

	public static final String subscriptionIDKey = "subscription_id";
	private static final long defaultSubscriptionID = 1;

    private static BackgroundTransformation mImageTransformation;

	private Subscription mSubscription = null;
    private View fragmentView;

    private Activity mActivity;

    protected FeedRefreshLayout mFeedSwipeRefreshView;

    private FeedRecyclerView mRecyclerView;
    private TextView mTitleView;
    private FeedViewAdapter mAdapter;
    private FeedCursorLoader mCursorLoader;
    private RelativeLayoutWithBackground mTopContainer;
    private ImageView mContainerBackground;
    private View mFloatingButton;

    private static BackgroundTransformation mBackgroundTransformation;

    private Palette mPalette = null;

    private FragmentUtils mFragmentUtils;

    private final Target mTarget = new Target() {

        @Override
        @TargetApi(16)
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (Build.VERSION.SDK_INT < 16)
                return;

            BitmapDrawable ob = new BitmapDrawable(mContainerBackground.getResources(),bitmap);
            //mContainerBackground.setBackground(ob);
            mContainerBackground.setImageDrawable(ob);
            String image = mSubscription.getImageURL();
            PaletteCache.generate(image, bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            return;
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            if (isAdded()) {
                mContainerBackground.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
            return;
        }
    };

	private void setSubscription(Subscription subscription) {
        mSubscription = subscription;
	}

    public void onAttach (Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
        //requeryLoader(mSubscription);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		fragmentView = inflater.inflate(R.layout.feed_view, container, false);

        mFloatingButton = fragmentView.findViewById(R.id.feedview_fap_button);
        mRecyclerView = (FeedRecyclerView) fragmentView.findViewById(R.id.feed_recycler_view);
        mTitleView = (TextView) fragmentView.findViewById(R.id.feed_title);
        //mItemBackground = (ImageView) fragmentView.findViewById(R.id.item_background);

        mFeedSwipeRefreshView = (FeedRefreshLayout) fragmentView.findViewById(R.id.feed_refresh_layout);
        mFeedSwipeRefreshView.setOnRefreshListener(this);
        mFeedSwipeRefreshView.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mFeedSwipeRefreshView.setRecyclerView(mRecyclerView);

        mTopContainer = (RelativeLayoutWithBackground)fragmentView.findViewById(R.id.top_container);

        mContainerBackground = (ImageView) fragmentView.findViewById(R.id.background_container);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        mFragmentUtils = new FragmentUtils(getActivity(), fragmentView, this);

		return fragmentView;
	}

    @Override
    public void onResume() {
        super.onResume();
        PaletteObservable.registerListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PaletteObservable.unregisterListener(this);
    }

    @Override
    public void onRefresh() {
        Log.d("FeedRefresh", "starting");
        mFeedSwipeRefreshView.setRefreshing(true);
        PodcastDownloadManager.start_update(mActivity, mSubscription, this);
    }

    @Override
    public void complete(boolean succes) {
        Log.d("FeedRefresh", "ending");
        mFeedSwipeRefreshView.setRefreshing(false);
    }

	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCursorLoader = new FeedCursorLoader(this, (FeedViewAdapter)mAdapter, mCursor, mSubscription);
        mCursorLoader.requery();
    }

    public void requeryLoader(@NonNull Subscription subscription)
    {
        mCursorLoader.setSubscription(subscription);
        mCursorLoader.requery();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new FeedViewAdapter(mActivity, this, mCursor);
        mRecyclerView.setAdapter(mAdapter);

        //bindNewSubscrption(mSubscription, false);
        if (mBackgroundTransformation == null) {
            mBackgroundTransformation = new BackgroundTransformation(mActivity, mContainerBackground.getLayoutParams().height);
        }

        if (mSubscription != null) {

            mTitleView.setText(mSubscription.getTitle());

            String image = mSubscription.getImageURL();
            if (image != null && image != "") {
                //PicassoWrapper.simpleLoad(mActivity, image, mTarget);
                PicassoWrapper.load(mActivity, image, mTarget, mBackgroundTransformation);

                mPalette = PaletteCache.get(image);
            }
        }

        if (mPalette != null)
            setColors(mPalette);
    }

	public static FeedFragment newInstance(Subscription subscription) {
		FeedFragment fragment = new FeedFragment();
		fragment.mSubscription = subscription;

		return fragment;
	}

	@Override
	public String getWhere() {
		String where = ItemColumns.SUBS_ID + "=" + mSubscription.getId();
		return where;
	}

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        setColors(argChangedPalette);
    }

    @Override
    public String getPaletteUrl() {
        return mSubscription.getImageURL();
    }

    private void setColors(Palette argChangedPalette) {

        Palette.Swatch muted = argChangedPalette.getMutedSwatch();

        if (muted != null)
            mTitleView.setBackgroundColor(muted.getBodyTextColor());

        mAdapter.setPalette(argChangedPalette);
    }

    public void bindNewSubscrption(Subscription subscription, boolean argRequery) {
        mSubscription = subscription;

        if (argRequery) {
            requeryLoader(mSubscription);
        }
    }
}
