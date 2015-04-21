package org.bottiger.podcast;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Trace;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.bottiger.podcast.adapters.FeedViewAdapter;
import org.bottiger.podcast.images.FrescoHelper;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.playlist.FeedCursorLoader;
import org.bottiger.podcast.playlist.ReorderCursor;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.WhitenessUtils;
import org.bottiger.podcast.views.FeedRecyclerView;
import org.bottiger.podcast.views.FloatingActionButton;
import org.bottiger.podcast.views.MultiShrink.feed.MultiShrinkScroller;
import org.bottiger.podcast.views.MultiShrink.feed.QuickFeedImage;
import org.bottiger.podcast.views.MultiShrink.feed.SchedulingUtils;

import java.util.ArrayList;

/**
 * Created by apl on 14-02-2015.
 */
public class FeedActivity extends ActionBarActivity implements PaletteListener {

    public static final int MODE_FULLY_EXPANDED = 4;

    private static final String TAG = "FeedActivity";

    private static final String KEY_THEME_COLOR = "theme_color";

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int DEFAULT_SCRIM_ALPHA = 0xC8;
    private static final int SCRIM_COLOR = Color.argb(DEFAULT_SCRIM_ALPHA, 0, 0, 0);

    private int mStatusBarColor;
    private int mExtraMode = MODE_FULLY_EXPANDED;
    private boolean mHasAlreadyBeenOpened;

    private boolean mExpandedLayout = false;

    private QuickFeedImage mPhotoView;
    private RecyclerView mRecyclerView;
    private MultiShrinkScroller mMultiShrinkScroller;
    private FloatingActionButton mFloatingButton;
    private FeedViewAdapter mAdapter;
    private FeedCursorLoader mCursorLoader;
    protected ReorderCursor mCursor = null;
    private String mUrl;

    /**
     *  This scrim's opacity is controlled in two different ways. 1) Before the initial entrance
     *  animation finishes, the opacity is animated by a value animator. This is designed to
     *  distract the user from the length of the initial loading time. 2) After the initial
     *  entrance animation, the opacity is directly related to scroll position.
     */
    private ColorDrawable mWindowScrim;
    private boolean mIsEntranceAnimationFinished;
    //private MaterialColorMapUtils mMaterialColorMapUtils;
    private boolean mIsExitAnimationInProgress;
    private boolean mHasComputedThemeColor;

    public static final String FEED_ACTIVITY_IS_SLIM = "SlimActivity";
    public static final String SUBSCRIPTION_URL_KEY = "url";
    public static final String SUBSCRIPTION_SLIM_KEY = "SlimSubscription";
    public static final String EPISODES_SLIM_KEY = "SlimEpisodes";

    private ISubscription mSubscription = null;

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
            mIsExitAnimationInProgress = true;
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

    public static void start(@NonNull Activity argActivity, @NonNull String argURL) {
        Bundle b = new Bundle();
        b.putBoolean(FEED_ACTIVITY_IS_SLIM, false);
        b.putString(FeedActivity.SUBSCRIPTION_URL_KEY, argURL);
        startActivity(argActivity, b);
    }

    public static void startSlim(@NonNull Activity argActivity, @NonNull String argURL, @Nullable SlimSubscription argSubscription) {
        Bundle b = new Bundle();
        b.putBoolean(FEED_ACTIVITY_IS_SLIM, true);
        b.putString(FeedActivity.SUBSCRIPTION_URL_KEY, argURL);
        b.putParcelable(SUBSCRIPTION_SLIM_KEY, argSubscription); // Not required, but nice to have if we already got it
        startActivity(argActivity, b);
    }

    private static void startActivity(@NonNull Activity argActivity, @NonNull Bundle argBundle) {
        Intent intent = new Intent(argActivity, FeedActivity.class);
        intent.putExtras(argBundle);
        argActivity.startActivity(intent);
        argActivity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 18) {
            Trace.beginSection("onCreate()");
        }
        mSubscription = null;

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        processIntent(getIntent());

        if (mSubscription == null) {
            throw new IllegalStateException("Episode can not be null");
        }

        if (mSubscription.IsDirty() && mSubscription.getType() == ISubscription.TYPE.DEFAULT) {
            ((Subscription)mSubscription).update(this.getContentResolver());
        }

        // Show QuickContact in front of soft input
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.feed_activity);

        //mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());


        mPhotoView = (QuickFeedImage) findViewById(R.id.photo);
        mMultiShrinkScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);
        mFloatingButton = (FloatingActionButton) findViewById(R.id.feedview_fap_button);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FrameLayout.MarginLayoutParams params = (FrameLayout.MarginLayoutParams) mMultiShrinkScroller.getLayoutParams();
            params.topMargin = ToolbarActivity.getStatusBarHeight(getResources());
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            mMultiShrinkScroller.setLayoutParams(params);
        }


        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpandedLayout = !mExpandedLayout;
                mAdapter.setExpanded(mExpandedLayout);
            }
        });


        mUrl = mSubscription.getImageURL();

        FrescoHelper.PalettePostProcessor postProcessor = new FrescoHelper.PalettePostProcessor(this, mUrl);
        FrescoHelper.loadImageInto(mPhotoView, mUrl, postProcessor);

        final View transparentView = findViewById(R.id.transparent_view);
        if (mMultiShrinkScroller != null) {
            transparentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMultiShrinkScroller.scrollOffBottom();
                }
            });
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.feed_view_toolbar);

        toolbar.setTitle(mSubscription.getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(mWindowScrim);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView = (FeedRecyclerView) findViewById(R.id.feed_recycler_view);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new FeedViewAdapter(this, mCursor);
        mRecyclerView.setAdapter(mAdapter);

        if (mSubscription.getType() == ISubscription.TYPE.DEFAULT) {
            mCursorLoader = new FeedCursorLoader(this, mAdapter, mCursor, (Subscription)mSubscription);
            mCursorLoader.requery();
        }

        mMultiShrinkScroller.initialize(mMultiShrinkScrollerListener, mExtraMode == MODE_FULLY_EXPANDED);
        mMultiShrinkScroller.setTitle(mSubscription.getTitle());

        // mMultiShrinkScroller needs to perform asynchronous measurements after initalize(), therefore
        // we can't mark this as GONE.
        //mMultiShrinkScroller.setVisibility(View.INVISIBLE);

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
                            // Need to wait for pre draw for setting the theme color. Setting the
                            // header tint before the MultiShrinkScroller has been measured will
                            // cause incorrect tinting calculations.
                            if (color != 0) {
                                /*
                                setThemeColor(mMaterialColorMapUtils
                                        .calculatePrimaryAndSecondaryColor(color));
                                        */
                            }
                        }
                    });
        }

        if (Build.VERSION.SDK_INT >= 18) {
            Trace.endSection();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PaletteObservable.registerListener(this);

        if (mSubscription.getType() == ISubscription.TYPE.DEFAULT) {
            if (((Subscription)mSubscription).getPaletteUrl() != null) {
                Palette palette = PaletteCache.get(((Subscription)mSubscription).getPaletteUrl());
                onPaletteFound(palette);
            }
        }
    }

    @Override
    protected void onStop() {
        PaletteObservable.unregisterListener(this);
        super.onStop();
    }

    private void processIntent(Intent argIntent) {
        Bundle b = getIntent().getExtras();
        String value = b.getString(SUBSCRIPTION_URL_KEY);
        mSubscription = Subscription.getByUrl(getContentResolver(), value);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feedview, menu);
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
                SoundWaves.sSubscriptionRefreshManager.refresh(mSubscription, new IDownloadCompleteCallback() {
                    @Override
                    public void complete(boolean succes) {
                        return;
                    }
                });
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
        mMultiShrinkScroller.setHeaderTintColor(extractor.getPrimary());
        mFloatingButton.onPaletteFound(argChangedPalette);
        UIUtils.tintStatusBar(extractor.getPrimary(), this);
        mStatusBarColor = extractor.getPrimary();
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
}
