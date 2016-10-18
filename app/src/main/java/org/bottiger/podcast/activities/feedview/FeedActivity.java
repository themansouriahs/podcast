package org.bottiger.podcast.activities.feedview;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.ToolbarActivity;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.activities.discovery.DiscoveryFeedActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.WhitenessUtils;
import org.bottiger.podcast.views.FeedRecyclerView;
import org.bottiger.podcast.views.FloatingActionButton;
import org.bottiger.podcast.views.MultiShrink.feed.FeedViewTopImage;
import org.bottiger.podcast.views.MultiShrink.feed.MultiShrinkScroller;
import org.bottiger.podcast.views.MultiShrink.feed.SchedulingUtils;
import org.bottiger.podcast.views.dialogs.DialogBulkDownload;
import org.bottiger.podcast.views.utils.SubscriptionSettingsUtils;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by apl on 14-02-2015.
 */
public class FeedActivity extends TopActivity implements PaletteListener {

    public static final int MODE_FULLY_EXPANDED = 4;

    private static final String TAG = "FeedActivity";
    private static final String KEY_THEME_COLOR = "theme_color";

    private Toolbar mToolbar;

    // Feed Settings
    private SubscriptionSettingsUtils mSubscriptionSettingsUtils;
    private boolean mSettingsRevealed = false;
    private final int SETTINGS_REVEAL_DURATION = 200; // in ms
    private SupportAnimator mRevealAnimator;

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int DEFAULT_SCRIM_ALPHA = 0xC8;
    private static final int SCRIM_COLOR = Color.argb(DEFAULT_SCRIM_ALPHA, 0, 0, 0);

    private int mStatusBarColor;
    private int mExtraMode = MODE_FULLY_EXPANDED;
    private boolean mHasAlreadyBeenOpened;

    private FeedViewTopImage mPhotoView;
    private RecyclerView mRecyclerView;
    private TextView mNoEpisodesTextView;
    protected MultiShrinkScroller mMultiShrinkScroller;
    protected FloatingActionButton mFloatingButton;
    private FrameLayout mRevealLayout;
    private FeedViewAdapter mAdapter;
    private String mUrl;

    private boolean mIsSlimSubscription = false;
    private rx.Subscription mRxSubscription;

    /**
     *  This scrim's opacity is controlled in two different ways. 1) Before the initial entrance
     *  animation finishes, the opacity is animated by a value animator. This is designed to
     *  distract the user from the length of the initial loading time. 2) After the initial
     *  entrance animation, the opacity is directly related to scroll position.
     */
    private ColorDrawable mWindowScrim;
    private boolean mIsEntranceAnimationFinished;

    public static final String FEED_ACTIVITY_EXTRA = "FeedActivityExtra";
    public static final String FEED_ACTIVITY_IS_SLIM = "SlimActivity";
    public static final String SUBSCRIPTION_URL_KEY = "url";
    public static final String SUBSCRIPTION_SLIM_KEY = "SlimSubscription";
    public static final String EPISODES_SLIM_KEY = "SlimEpisodes";

    protected ISubscription mSubscription = null;
    protected ProgressDialog mProgress;

    final MultiShrinkScroller.MultiShrinkScrollerListener mMultiShrinkScrollerListener
            = new MultiShrinkScroller.MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onEnterFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {

        }

        @Override
        public void onEntranceAnimationDone() {
            mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {
            if (mIsEntranceAnimationFinished) {
                mWindowScrim.setAlpha((int) (0xFF * ratio));
            }
        }
    };

    SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            //.collapseActionView();
            return false;
        }
        @Override
        public boolean onQueryTextChange(String argSearchQuery) {

            mMultiShrinkScroller.collapseHeader();

            FeedViewAdapter adapter = mAdapter;
            if (adapter != null) {
                adapter.search(argSearchQuery);
            }

            return false;
        }
    };

    protected IDownloadCompleteCallback mRefreshCompleteCallback = new IDownloadCompleteCallback() {
        @Override
        public void complete(boolean argSucces, ISubscription argSubscription) {
            mProgress.dismiss();

            if (!argSucces)
                return;

            // FIXME why do I get Subscriptions here?
            if (argSubscription instanceof Subscription)
                return;

            final SlimSubscription slimSubscription = (SlimSubscription)argSubscription;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setViewState(slimSubscription);
                    mAdapter.setDataset(slimSubscription);
                }
            });
        }
    };

    public static void start(@NonNull Activity argActivity, @NonNull Subscription argSubscription) {
        if (!argSubscription.IsLoaded()) {
            SoundWaves.getAppContext(argActivity).getLibraryInstance().loadEpisodes(argSubscription);
        }
        Bundle b = new Bundle();
        b.putBoolean(FEED_ACTIVITY_IS_SLIM, false);
        b.putString(FeedActivity.SUBSCRIPTION_URL_KEY, argSubscription.getURLString());
        startActivity(argActivity, b);
    }

    public static void startSlim(@NonNull Activity argActivity, @NonNull String argURL, @Nullable SlimSubscription argSubscription) {
        Bundle b = new Bundle();
        Intent intent = new Intent(argActivity, DiscoveryFeedActivity.class);

        b.putBoolean(FEED_ACTIVITY_IS_SLIM, true);
        b.putString(FeedActivity.SUBSCRIPTION_URL_KEY, argURL);
        b.putParcelable(SUBSCRIPTION_SLIM_KEY, argSubscription); // Not required, but nice to have if we already got it

        intent.putExtras(b);

        argActivity.startActivity(intent);
        argActivity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
    }

    private static void startActivity(@NonNull Activity argActivity, @NonNull Bundle argBundle) {
        Intent intent = new Intent(argActivity, FeedActivity.class);
        intent.putExtras(argBundle);
        argActivity.startActivity(intent);
        argActivity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mSubscription = null;

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        processIntent();

        if (mSubscription == null) {
            VendorCrashReporter.report("FeedActivity", "Subscription can not be null");
            return;
        }

        Log.d(TAG, "Showing: " + mSubscription);

        setContentView(R.layout.feed_activity);

        mAdapter = getAdapter();

        mRxSubscription = subscribeToChanges(mSubscription, mAdapter);

        mPhotoView = (FeedViewTopImage) findViewById(R.id.photo);
        mNoEpisodesTextView = (TextView) findViewById(R.id.feed_recycler_view_empty);
        mMultiShrinkScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);
        mFloatingButton = (FloatingActionButton) findViewById(R.id.feedview_fap_button);
        mRevealLayout = (FrameLayout) findViewById(R.id.feed_activity_settings_container);
        mRecyclerView = (FeedRecyclerView) findViewById(R.id.feed_recycler_view);

        setViewState(mSubscription);

        if (mSubscription instanceof Subscription) {
            mSubscriptionSettingsUtils = new SubscriptionSettingsUtils(mRevealLayout, (Subscription)mSubscription);
            mAdapter.setExpanded(((Subscription) mSubscription).isShowDescription());

            mSubscriptionSettingsUtils.setListOldestFirstListener(new SubscriptionSettingsUtils.OnSettingsChangedListener() {
                @Override
                public void OnSettingsChanged(boolean isChecked) {
                    @FeedViewAdapter.Order int sortOrder = mSubscription.isListOldestFirst() ?  FeedViewAdapter.OLDEST_FIRST : FeedViewAdapter.RECENT_FIRST;
                    mAdapter.setOrder(sortOrder);
                }
            });
            @FeedViewAdapter.Order int sortOrder = mSubscription.isListOldestFirst() ?  FeedViewAdapter.OLDEST_FIRST : FeedViewAdapter.RECENT_FIRST;
            mAdapter.setOrder(sortOrder);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FrameLayout.MarginLayoutParams params = (FrameLayout.MarginLayoutParams) mMultiShrinkScroller.getLayoutParams();
            params.topMargin = ToolbarActivity.getStatusBarHeight(getResources());
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            mMultiShrinkScroller.setLayoutParams(params);
        }

        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the center for the clipping circle
                int cx = (mRevealLayout.getLeft() + mRevealLayout.getRight());
                int cy = (mRevealLayout.getTop() + mRevealLayout.getBottom());

                // get the final radius for the clipping circle
                int revealRadius = Math.max(mRevealLayout.getWidth(), mRevealLayout.getHeight());


                if (mSettingsRevealed) {
                    mRevealAnimator =
                            ViewAnimationUtils.createCircularReveal(mRevealLayout, cx, cy, revealRadius, 0);
                    mRevealAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                    mRevealAnimator.setDuration(SETTINGS_REVEAL_DURATION);
                    mRevealAnimator.addListener(new SupportAnimator.AnimatorListener() {
                        @Override
                        public void onAnimationStart() {
                        }

                        @Override
                        public void onAnimationCancel() {
                        }

                        @Override
                        public void onAnimationRepeat() {
                        }

                        @Override
                        public void onAnimationEnd() {
                            mRevealLayout.setVisibility(View.INVISIBLE);
                            setFABDrawable(R.drawable.ic_tune_white);
                            mToolbar.setTitle(mSubscription.getTitle());
                        }
                    });
                    mRevealAnimator.start();
                } else {

                    // Open Settings
                    mMultiShrinkScroller.expandHeader();
                    mRevealAnimator =
                            ViewAnimationUtils.createCircularReveal(mRevealLayout, cx, cy, 0, revealRadius);
                    mRevealAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                    mRevealAnimator.setDuration(SETTINGS_REVEAL_DURATION);

                    mRevealLayout.setVisibility(View.VISIBLE);
                    setFABDrawable(R.drawable.ic_clear_white);
                    mToolbar.setTitle(R.string.menu_settings);
                    mRevealAnimator.start();
                }
                mSettingsRevealed = !mSettingsRevealed;
            }
        });

        mUrl = mSubscription.getImageURL();

        mPhotoView.setBackgroundColor(mSubscription.getPrimaryColor());
        ColorDrawable cd = new ColorDrawable(mSubscription.getPrimaryColor());

        Glide.with(this)
                .load(mUrl)
                .asBitmap()
                .placeholder(cd)
                .fitCenter()
                .into(new BitmapImageViewTarget(mPhotoView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        boolean isWhite = WhitenessUtils.isBitmapWhiteAtTopOrBottom(resource);
                        mMultiShrinkScroller.setUseGradient(isWhite);
                        mPhotoView.setImageBitmap(resource);
                    }
                });

        final View transparentView = findViewById(R.id.transparent_view);
        if (mMultiShrinkScroller != null) {
            transparentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMultiShrinkScroller.scrollOffBottom();
                }
            });
        }

        mToolbar = (Toolbar) findViewById(R.id.feed_view_toolbar);

        mToolbar.setTitle(mSubscription.getTitle());
        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeButtonEnabled(true);
        }

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(mWindowScrim);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setBackgroundColor(ColorUtils.getBackgroundColor(this));

        mRecyclerView.setAdapter(mAdapter);

        mMultiShrinkScroller.initialize(mMultiShrinkScrollerListener, mExtraMode == MODE_FULLY_EXPANDED);
        mMultiShrinkScroller.setTitle(mSubscription.getTitle());

        SchedulingUtils.doOnPreDraw(mMultiShrinkScroller, /* drawNextFrame = */ true,
                new Runnable() {
                    @Override
                    public void run() {
                        if (!mHasAlreadyBeenOpened) {
                            // The initial scrim opacity must match the scrim opacity that would be
                            // achieved by scrolling to the starting position.
                            final float alphaRatio = mExtraMode == MODE_FULLY_EXPANDED ?
                                    1 : mMultiShrinkScroller.getStartingTransparentHeightRatio();
                            final int duration = getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);
                            final int desiredAlpha = (int) (0xFF * alphaRatio);
                            ObjectAnimator o = ObjectAnimator.ofInt(mWindowScrim, "alpha", 0,
                                    desiredAlpha).setDuration(duration);

                            o.start();
                        }
                    }
                });

        if (savedInstanceState != null) {
            final int color = savedInstanceState.getInt(KEY_THEME_COLOR, 0);
            SchedulingUtils.doOnPreDraw(mMultiShrinkScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Need to wait for the pre draw before setting the initial scroll
                            // value. Prior to pre draw all scroll values are invalid.
                            if (mHasAlreadyBeenOpened) {
                                mMultiShrinkScroller.setVisibility(View.VISIBLE);
                                mMultiShrinkScroller.setScroll(mMultiShrinkScroller.getScrollNeededToBeFullScreen());
                            }
                        }
                    });
        }
    }

    @NonNull
    protected FeedViewAdapter getAdapter() {
        FeedViewAdapter adapter = new FeedViewAdapter(this, mSubscription);
        return adapter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRxSubscription == null)
            return;

        if (mRxSubscription.isUnsubscribed())
            mRxSubscription.unsubscribe();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mSubscription == null) {
            Log.w(TAG, "FeedActivitySubscription is null");
            VendorCrashReporter.report(TAG, "FeedActivitySubscription is null");
            finish();
            return;
        }

        if (mSubscription.getType() == ISubscription.DEFAULT) {
            String url = mSubscription.getImageURL();
            if (url != null) {
                PaletteHelper.generate(url, this, this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void processIntent() {
        Bundle b = getIntent().getExtras();

        if (b == null)
            return;

        boolean isSlim = b.getBoolean(FEED_ACTIVITY_IS_SLIM);
        String url = b.getString(SUBSCRIPTION_URL_KEY);

        if (isSlim) {
            mSubscription = b.<SlimSubscription>getParcelable(SUBSCRIPTION_SLIM_KEY);
            mIsSlimSubscription = true;
        } else {
            mSubscription = SoundWaves.getAppContext(this).getLibraryInstance().getSubscription(url);
            mIsSlimSubscription = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feedview, menu);

        final MenuItem myActionMenuItem = menu.findItem( R.id.action_search);
        final SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(mOnQueryTextListener);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                mMultiShrinkScroller.scrollOffBottom();
                return true;
            case R.id.menu_refresh_feed:
                SoundWaves.getAppContext(this).getRefreshManager().refresh(mSubscription, new IDownloadCompleteCallback() {
                    @Override
                    public void complete(boolean succes, ISubscription argCallback) {
                        return;
                    }
                });
                return true;
            case R.id.menu_bulk_download:
                Playlist playlist = SoundWaves.getAppContext(this).getPlaylist();
                if (mSubscription instanceof Subscription) {
                    DialogBulkDownload dialogBulkDownload = new DialogBulkDownload();
                    Dialog dialog = dialogBulkDownload.onCreateDialog(this, playlist, (Subscription)mSubscription);
                    dialog.show();
                } else {
                    VendorCrashReporter.report("Bulk download", "Bulk download can not be started");
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateStatusBarColor() {
        if (mMultiShrinkScroller == null) {
            return;
        }
        final int desiredStatusBarColor;
        // Only use a custom status bar color if QuickContacts touches the top of the viewport.
        if (mMultiShrinkScroller.getScrollNeededToBeFullScreen() <= 0) {
            desiredStatusBarColor = mStatusBarColor;
        } else {
            desiredStatusBarColor = Color.TRANSPARENT;
        }

        // Animate to the new color.
        if (Build.VERSION.SDK_INT >= 21) {
            final ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor",
                    getWindow().getStatusBarColor(), desiredStatusBarColor);
            animation.setDuration(ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION);
            animation.setEvaluator(new ArgbEvaluator());
            animation.start();
        }
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(this, argChangedPalette);

        int tintColor = ColorUtils.adjustToThemeDark(getResources(), argChangedPalette, extractor.getPrimary());

        // extractor.getPrimary()
        mMultiShrinkScroller.setHeaderTintColor(tintColor);
        mFloatingButton.onPaletteFound(argChangedPalette);
        @ColorInt int feedColor = tintColor;
        mRevealLayout.setBackgroundColor(feedColor);
        UIUtils.tintStatusBar(feedColor, this);
        mStatusBarColor = tintColor;
        mAdapter.setPalette(argChangedPalette);
    }

    @Override
    public String getPaletteUrl() {
        return mSubscription.getImageURL();
    }

    /**
     * Examine how many white pixels are in the bitmap in order to determine whether or not
     * we need gradient overlays on top of the image.
     */
    private void analyzeWhitenessOfPhotoAsynchronously(@NonNull final Bitmap argBitmap) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return WhitenessUtils.isBitmapWhiteAtTopOrBottom(argBitmap);
            }

            @Override
            protected void onPostExecute(Boolean isWhite) {
                super.onPostExecute(isWhite);
                mMultiShrinkScroller.setUseGradient(isWhite);
            }
        }.execute();
    }

    private void setViewState(@Nullable ISubscription argSubscription) {
        boolean isEmpty = argSubscription == null || argSubscription.getEpisodes().size() == 0;
        mNoEpisodesTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(!isEmpty ? View.VISIBLE : View.GONE);
    }

    private rx.Subscription subscribeToChanges(@NonNull final ISubscription argSubscription,
                                               @NonNull final FeedViewAdapter argAdapter) {
        return SoundWaves.getRxBus()
                .toObserverable()
                .ofType(SubscriptionChanged.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SubscriptionChanged>() {
                    @Override
                    public void call(SubscriptionChanged subscriptionChanged) {
                        @SubscriptionChanged.Action int action = subscriptionChanged.getAction();
                        boolean doNotify = action ==
                                SubscriptionChanged.ADDED ||
                                action == SubscriptionChanged.REMOVED ||
                                action == SubscriptionChanged.LOADED;

                        if (doNotify) {
                            setViewState(argSubscription);
                            argAdapter.notifyEpisodesChanged();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d("FeedViewAdapter", "error: " + throwable.toString());
                    }
                });
    }

    protected void setFABDrawable(@DrawableRes int argRes) {
        if (mFloatingButton.getVisibility() == View.GONE)
            return;

        mFloatingButton.setImageDrawable(getResources().getDrawable(argRes));
    }
}
