package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v7.graphics.Palette;
import android.transition.ChangeBounds;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.rxbus.RxBus;
import org.bottiger.podcast.utils.rxbus.RxBusSimpleEvents;
import org.bottiger.podcast.views.dialogs.DialogPlaybackSpeed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.bottiger.podcast.views.PlayerButtonView.StaticButtonColor;

/**
 * Created by apl on 30-09-2014.
 */
public class TopPlayer extends RelativeLayout implements PaletteListener, ScrollingView, NestedScrollingChild {

    private static final String TAG = "TopPlayer";

    private final NestedScrollingChildHelper scrollingChildHelper;
    private ViewConfiguration mViewConfiguration;  // getScaledTouchSlop

    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;

    private static long ANIMATION_DURATION = 100L;

    @IntDef({SMALL, MEDIUM, LARGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerLayout {}

    private @PlayerLayout int mPlayerLayout = LARGE;

    private int mPlayPauseLargeSize = -1;

    private Context mContext;

    public static int sizeSmall                 =   -1;
    public static int sizeMedium                =   -1;
    public static int sizeLarge                 =   -1;
    public static int sizeActionbar             =   -1;

    public static int sizeStartShrink           =   -1;
    public static int sizeShrinkBuffer           =   -1;

    public static int sizeImageContainer        =   -1;

    private int mSeekbarDeadzone                =   20; // dp
    private int mSeekbarFadeDistance            =   20; // dp
    private int mTextInfoFadeDistance           =   100; // dp
    private int mTextFadeDistance               =   20; // dp

    private float screenHeight;

    private int mSeekbarDeadzonePx;
    private int mSeekbarFadeDistancePx;
    private int mTextFadeDistancePx;
    private int mTextInfoFadeDistancePx;

    private boolean mCanScrollUp = true;

    private String mDoFullscreentKey;
    private boolean mFullscreen = false;

    private String mDoDisplayTextKey;
    private boolean mDoDisplayText = false;

    private PlayerRelativeLayout mPlayerControlsLinearLayout;

    private RelativeLayout mPlayerButtons;
    private int mPlayerButtonsHeight = -1;

    private int mCenterSquareMarginTop = -1;
    private float mCenterSquareMargin = -1;

    private TopPlayer mLayout;
    private ImageButton mExpandEpisode;
    private View mGradient;
    private View mEpisodeText;
    private View mEpisodeInfo;
    private ImageViewTinted mPhoto;
    private PlayPauseImageView mPlayPauseButton;
    private View mImageContainer;

    private PlayerSeekbar mSeekbar;

    private View mForwardButton;
    private View mBackButton;
    private View mDownloadButton;
    private View mQueueButton;
    private View mFavoriteButton;
    private PlayerButtonView mFullscreenButton;
    private PlayerButtonView mSleepButton;
    private Button mSpeedpButton;

    private PlayerLayoutParameter mSmallLayout = new PlayerLayoutParameter();
    private PlayerLayoutParameter mLargeLayout = new PlayerLayoutParameter();

    private GestureDetectorCompat mGestureDetector;
    private TopPLayerScrollGestureListener mTopPLayerScrollGestureListener;

    private CompositeSubscription mRxSubscriptions;

    private @ColorInt int mBackgroundColor = -1;

    private SharedPreferences prefs;

    private class PlayerLayoutParameter {
        public int SeekBarLeftMargin;
        public int PlayPauseSize;
        public int PlayPauseBottomMargin;
    }

    public TopPlayer(Context context) {
        super(context, null);
        scrollingChildHelper = new NestedScrollingChildHelper(this);
        //init(context);
    }

    public TopPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        scrollingChildHelper = new NestedScrollingChildHelper(this);
        init(context);
    }

    public TopPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scrollingChildHelper = new NestedScrollingChildHelper(this);
        init(context);
    }

    @TargetApi(21)
    public TopPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        scrollingChildHelper = new NestedScrollingChildHelper(this);
        init(context);
    }

    private void init(@NonNull Context argContext) {

        mTopPLayerScrollGestureListener = new TopPLayerScrollGestureListener();
        scrollingChildHelper.setNestedScrollingEnabled(true);

        mGestureDetector = new GestureDetectorCompat(argContext,mTopPLayerScrollGestureListener);

        mViewConfiguration = ViewConfiguration.get(argContext);

        int screenHeight = 1000;

        if (isInEditMode()) {
            return;
        }

        mContext = argContext;

        prefs = PreferenceManager.getDefaultSharedPreferences(argContext.getApplicationContext());
        mDoDisplayTextKey = argContext.getResources().getString(R.string.pref_top_player_text_expanded_key);
        mDoDisplayText = prefs.getBoolean(mDoDisplayTextKey, mDoDisplayText);

        mDoFullscreentKey = argContext.getResources().getString(R.string.pref_top_player_fullscreen_key);
        mFullscreen = prefs.getBoolean(mDoFullscreentKey, mFullscreen);


        sizeShrinkBuffer = (int) UIUtils.convertDpToPixel(400, argContext);
        screenHeight = UIUtils.getScreenHeight(argContext);

        sizeSmall = argContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_minimum);
        sizeMedium = argContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_medium);
        sizeLarge = screenHeight- argContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_maximum_bottom);

        sizeStartShrink = sizeSmall+sizeShrinkBuffer;

        mSeekbarDeadzonePx        = (int)UIUtils.convertDpToPixel(mSeekbarDeadzone, argContext);
        mSeekbarFadeDistancePx    = (int)UIUtils.convertDpToPixel(mSeekbarFadeDistance, argContext);
        mTextFadeDistancePx       = (int)UIUtils.convertDpToPixel(mTextFadeDistance, argContext);
        mTextInfoFadeDistancePx   = (int)UIUtils.convertDpToPixel(mTextInfoFadeDistance, argContext);

        sizeActionbar = argContext.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLayout = (TopPlayer) findViewById(R.id.session_photo_container);

        mPlayerControlsLinearLayout = (PlayerRelativeLayout)findViewById(R.id.expanded_controls);

        mPhoto = (ImageViewTinted) findViewById(R.id.session_photo);

        mPlayPauseButton = (PlayPauseImageView) findViewById(R.id.play_pause_button);
        mExpandEpisode = (ImageButton)findViewById(R.id.episode_expand);
        mEpisodeText = findViewById(R.id.episode_title);
        mEpisodeInfo = findViewById(R.id.episode_info);
        mImageContainer = findViewById(R.id.top_player_center_square);
        mForwardButton = findViewById(R.id.fast_forward_button);
        mBackButton = findViewById(R.id.rewind_button);
        mFullscreenButton = (PlayerButtonView) findViewById(R.id.fullscreen_button);
        mDownloadButton = findViewById(R.id.download);
        mQueueButton = findViewById(R.id.queue);
        mFavoriteButton = findViewById(R.id.bookmark);
        mGradient = findViewById(R.id.top_gradient_inner);
        mSleepButton = (PlayerButtonView) findViewById(R.id.sleep_button);
        mSpeedpButton = (Button) findViewById(R.id.speed_button);

        mPlayerButtons = (RelativeLayout) findViewById(R.id.player_buttons);

        mPlayPauseLargeSize = mPlayPauseButton.getLayoutParams().height;

        mCenterSquareMarginTop = (int)getResources().getDimension(R.dimen.top_player_center_square_margin_top);
        mCenterSquareMargin = getResources().getDimension(R.dimen.top_player_center_square_margin);

        mLargeLayout.SeekBarLeftMargin = 0;
        mLargeLayout.PlayPauseSize = mPlayPauseLargeSize;
        mLargeLayout.PlayPauseBottomMargin = ((RelativeLayout.LayoutParams)mPlayPauseButton.getLayoutParams()).bottomMargin;

        if (!mDoDisplayText) {
            mExpandEpisode.setImageResource(R.drawable.ic_expand_more_white);
            mEpisodeText.setVisibility(GONE);
            mGradient.setVisibility(GONE);
            mEpisodeInfo.setVisibility(GONE);
        }

        mSleepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sleepButtonPressed();
            }
        });

        setFullscreen(mFullscreen, false);

        mExpandEpisode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mDoDisplayText = !mDoDisplayText;
                    if (Build.VERSION.SDK_INT >= 19)
                        TransitionManager.beginDelayedTransition(mPlayerControlsLinearLayout);

                    if (mDoDisplayText) {
                        Log.d(TAG, "ShowText");
                        showText();
                    } else {
                        Log.d(TAG, "HideText");
                        hideText();
                    }
                } finally {
                    prefs.edit().putBoolean(mDoDisplayTextKey, mDoDisplayText).commit();
                }
            }
        });

        mFullscreenButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mFullscreen = !mFullscreen;
                    setFullscreen(mFullscreen, true);
                } finally {
                    prefs.edit().putBoolean(mDoFullscreentKey, mFullscreen).apply();
                }
            }
        });

        // Need to be called after the ndk player is connected
        String key = getResources().getString(R.string.pref_use_soundengine_key);
        boolean useCustomEngine = prefs.getBoolean(key, false);
        int visibility = useCustomEngine ? View.VISIBLE : View.GONE;
        mSpeedpButton.setVisibility(visibility);

        PlayerService ps = PlayerService.getInstance();
        if (ps != null) {
            SoundWavesPlayer player = ps.getPlayer();
            mSpeedpButton.setText(player.getCurrentSpeedMultiplier() + "X");
        }

        mSpeedpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = ((MainActivity)getContext());
                DialogPlaybackSpeed dialogPlaybackSpeed = DialogPlaybackSpeed.newInstance(DialogPlaybackSpeed.EPISODE);
                dialogPlaybackSpeed.show(activity.getFragmentManager(), DialogPlaybackSpeed.class.getName());
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        RxBus bus = SoundWaves.getRxBus();

        bus.toObserverable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object event) {
                        if (event instanceof RxBusSimpleEvents.PlaybackSpeedChanged) {
                            RxBusSimpleEvents.PlaybackSpeedChanged playbackSpeedChanged = (RxBusSimpleEvents.PlaybackSpeedChanged) event;
                            mSpeedpButton.setText(playbackSpeedChanged.speed + "X");
                        }
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRxSubscriptions.unsubscribe();
    }

    private void sleepButtonPressed() {
        try {
            PlayerService ps = PlayerService.getInstance();
            int minutes = 30;
            int onemin = 1000 * 60;
            ps.FaceOutAndStop(onemin*minutes);
            String toast = getResources().getQuantityString(R.plurals.player_sleep, minutes, minutes);
            Toast.makeText(mContext, toast, Toast.LENGTH_LONG).show();
        } catch (NullPointerException npe) {
            Log.d(TAG, "Could not connect to the Player");
        }
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private boolean minimalEnsured = false;

    public void ensureMinimalLayout() {
        if (minimalEnsured)
            return;

        Log.d("TopPlayer", "ensure minimum");

        int argScreenHeight = sizeSmall;
        int trans = sizeSmall-sizeLarge;
        //setPlayerHeight(sizeSmall, sizeSmall-sizeLarge);
        setTranslationY(trans);

        // Set seekbar visibility
        setSeekbarVisibility(argScreenHeight);

        // Set TextBox visibility
        setTextVisibility(argScreenHeight);
        setTextInfoVisibility(argScreenHeight);

        translatePhotoY(mPhoto, trans);

        /*
        mPlayerControlsLinearLayout.setTranslationY(-trans);
        mPlayPauseButton.setTranslationY(-trans);
        mForwardButton.setTranslationY(-trans);
        */

        minimalEnsured = true;
    }

    private boolean maximumEnsured = false;
    public void ensureMaximumLayout() {
        if (maximumEnsured)
            return;

        Log.d("TopPlayer", "ensure maximum");
        int argScreenHeight = sizeLarge;
        int trans = 0;
        //setPlayerHeight(sizeSmall, sizeSmall-sizeLarge);
        setTranslationY(trans);

        // Set seekbar visibility
        setSeekbarVisibility(argScreenHeight);

        // Set TextBox visibility
        setTextVisibility(argScreenHeight);
        setTextInfoVisibility(argScreenHeight);

        translatePhotoY(mPhoto, trans);
        mEpisodeText.setTranslationY(-trans);
        mEpisodeInfo.setTranslationY(-trans);

        mPlayerControlsLinearLayout.setTranslationY(0);
        mPlayPauseButton.setTranslationY(0);
        mForwardButton.setTranslationY(0);

        maximumEnsured = true;
    }

    public float getPlayerHeight() {
        return screenHeight;
    }

    public synchronized float scrollBy(float argY) {
        float oldHeight = getPlayerHeight();
        return setPlayerHeight(oldHeight - argY);
    }

    private void setNormalImageSize(float offset) {
        if (offset > 0)
            offset = 0;

        RelativeLayout.LayoutParams rlp;
        rlp = (LayoutParams) mImageContainer.getLayoutParams();
        rlp.width = sizeImageContainer;
        rlp.height = (int) (sizeImageContainer + offset);
        mImageContainer.setLayoutParams(rlp);
    }

    // returns the new height
    public float setPlayerHeight(float argScreenHeight) {
        Log.v(TAG, "Player height is set to: " + argScreenHeight);

        if (mFullscreen)
            return getHeight();

        if (sizeImageContainer <= 0) {
            //sizeImageContainer = mImageContainer.getHeight();
            float margin = getResources().getDimension(R.dimen.top_player_center_square_margin);
            sizeImageContainer = (int) (mLayout.getWidth() - 2*margin);
        }

        minimalEnsured = false;
        maximumEnsured = false;

        if (!validateState()) {
            return -1;
        }

        Log.v("TopPlayer", "setPlayerHeight: " +  argScreenHeight);

        screenHeight = argScreenHeight;
        float argOffset = argScreenHeight-sizeLarge;

        float transYControl = argOffset/2;

        setBackgroundVisibility(screenHeight);

        if (sizeImageContainer > 0) {
            setNormalImageSize(argOffset);
        }
        //mImageContainer.getLayoutParams().height
        /*
        mPlayerControlsLinearLayout.setTranslationY(transYControl);
        mPlayPauseButton.setTranslationY(transYControl);
        mImageContainer.setTranslationY(transYControl);
        mForwardButton.setTranslationY(transYControl);
        */

        //setTranslationY(transYControl);

        float minScreenHeight = screenHeight < sizeSmall ? sizeSmall : screenHeight;
        float minMaxScreenHeight = minScreenHeight > sizeLarge ? sizeLarge : minScreenHeight;
        screenHeight = minMaxScreenHeight;

        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = (int)minMaxScreenHeight;
        setLayoutParams(lp);


        if (System.currentTimeMillis() > 0) {
            return minMaxScreenHeight;
        }

        Log.d("setTranslationXYZ", "argScreenHeight: " +  argScreenHeight);

        float maxScreenHeight = argScreenHeight > sizeLarge ? sizeLarge : argScreenHeight;

        Log.d("setTranslationXYZ", "maxScreenHeight: " +  maxScreenHeight);

        float transY;
        float minMaxScreenHeight2 = maxScreenHeight;
        minMaxScreenHeight = minMaxScreenHeight < sizeSmall ? sizeSmall : minMaxScreenHeight;
        mCanScrollUp = maxScreenHeight > sizeSmall;

        Log.d("TopPlayerInput", "(canScrollUp: " + mCanScrollUp + ")maxScreenHeight -> " + maxScreenHeight + "ScreenOffset -> " + argOffset);

        //mPhoto.setTranslationY(-(argOffset/2));
        translatePhotoY(mPhoto, argOffset);

        mEpisodeText.setTranslationY(-argOffset);
        mEpisodeInfo.setTranslationY(-argOffset);

        //float transYControl = -argOffset < sizeStartShrink ? -argOffset : sizeStartShrink;
        transYControl = -1;

        if (minMaxScreenHeight > sizeStartShrink) {
            transYControl = -argOffset;
        } else {
            transYControl = sizeLarge-sizeStartShrink;
        }

        Log.d("TopPlayerInput", "transYControl: minMax->" + minMaxScreenHeight + " trans-> " + transYControl + " -arg-> " + -argOffset);

        mPlayerControlsLinearLayout.setTranslationY(transYControl);
        mPlayPauseButton.setTranslationY(transYControl);
        mImageContainer.setTranslationY(transYControl);
        mForwardButton.setTranslationY(transYControl);

        String size = "large";

        transY = minMaxScreenHeight-sizeLarge;
        Log.d("setTranslationXYZ", "minMaxScreenHeight: " +  minMaxScreenHeight);

        setTranslationY(transY);

        // Set seekbar visibility
        setSeekbarVisibility(argScreenHeight);

        // Set TextBox visibility
        setTextVisibility(argScreenHeight);
        setTextInfoVisibility(argScreenHeight);

        Log.d("setTranslationXYZ", size + ": " +  getLayoutParams().height + " trans: " + transY);

        Log.d("TopPlayerInputk", "layoutparams =>" + getLayoutParams().height + "Translation =>" + transY + " minMaxHeight =>" + minMaxScreenHeight);
        return minMaxScreenHeight; // return newVisibleHeight
    }

    public float setTextVisibility(float argTopPlayerHeight) {
        return setGenericVisibility(mEpisodeText, sizeMedium, mTextFadeDistancePx, argTopPlayerHeight);
    }

    public float setTextInfoVisibility(float argTopPlayerHeight) {
        return setGenericVisibility(mEpisodeInfo, sizeLarge, mTextInfoFadeDistancePx, argTopPlayerHeight);
    }

    public float setSeekbarVisibility(float argTopPlayerHeight) {
        return setGenericVisibility(mImageContainer, sizeMedium, mSeekbarFadeDistancePx, argTopPlayerHeight);
    }

    public float setBackgroundVisibility(float argTopPlayerHeight) {
        return setGenericVisibility(mPhoto, sizeStartShrink, sizeShrinkBuffer, argTopPlayerHeight);
    }

    public float setGenericVisibility(@NonNull View argView, int argVisibleHeight, int argFadeDistance, float argTopPlayerHeight) {
        Log.d("Top", "setGenericVisibility: h: " + argTopPlayerHeight);

        int VisibleHeightPx = argVisibleHeight;
        int InvisibleHeightPx = argVisibleHeight-argFadeDistance;

        float VisibilityFraction = 1;

        if (argTopPlayerHeight > VisibleHeightPx) {
            VisibilityFraction = 1f;
        } else if (argTopPlayerHeight < InvisibleHeightPx || argTopPlayerHeight == sizeSmall) {
            VisibilityFraction = 0f;
        } else {
            float thressholdDiff = VisibleHeightPx - InvisibleHeightPx;
            float DistanceFromVisible = argTopPlayerHeight - InvisibleHeightPx;

            VisibilityFraction = DistanceFromVisible / thressholdDiff;
        }

        argView.setAlpha(VisibilityFraction);

        return VisibilityFraction;
    }

    public int getVisibleHeight() {
        return (int) (getHeight()+getTranslationY());
    }

    private boolean validateState() {
        if (sizeSmall < 0 || sizeMedium < 0 || sizeLarge < 0) {
            Log.d("TopPlayer", "Layout sizes needs to be defined");
            return false;
        }
        return true;
    }

    public boolean isMinimumSize(int argHeight) {
        return sizeSmall >= argHeight;
    }

    public boolean isMinimumSize() {
        //return !mCanScrollUp;
        return sizeSmall >= getHeight()+getTranslationY();
    }

    public int getMinimumSize() {
        return sizeSmall;
    }

    public int getMaximumSize() {
        return sizeLarge;
    }

    public boolean isMaximumSize() {
        Log.d("MaximumSize", "Trans: " + getTranslationY());
        return mPlayerLayout == LARGE && getTranslationY() >= 0; // FIXME: refine
    }

    private void translatePhotoY(@NonNull View argImageView , float amount) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            argImageView.setTranslationY(-(amount/2));
        } else {
            //argImageView.setTranslationY(-amount);
        }
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        int color = ColorUtils.getBackgroundColor(getContext());
        mBackgroundColor = StaticButtonColor(mContext, argChangedPalette, color);
        setBackgroundColor(mBackgroundColor);
        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return mPlayPauseButton.getPaletteUrl();
    }

    private void hideText() {
        mExpandEpisode.setImageResource(R.drawable.ic_expand_more_white);

        fadeText(1.0f, 0.0f);
    }

    private void showText() {
        mExpandEpisode.setImageResource(R.drawable.ic_expand_less_white);
        mEpisodeText.setVisibility(VISIBLE);
        mGradient.setVisibility(VISIBLE);
        mEpisodeInfo.setVisibility(VISIBLE);

        fadeText(0.0f, 1.0f);
    }

    private void fadeText(float argAlphaStart, float argAlphaEnd) {
        mEpisodeText.setAlpha(argAlphaStart);
        mEpisodeInfo.setAlpha(argAlphaStart);
        mGradient.setAlpha(argAlphaStart);

        mEpisodeText.animate().alpha(argAlphaEnd);
        mEpisodeInfo.animate().alpha(argAlphaEnd);
        mGradient.animate().alpha(argAlphaEnd);
    }

    public @ColorInt int getBackGroundColor() {
        return mBackgroundColor;
    }

    public synchronized void setFullscreen(boolean argNewState, boolean doAnimate) {
        mFullscreen = argNewState;
        try {
            if (doAnimate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Transition trans = new ChangeBounds();
                trans.setDuration(ANIMATION_DURATION);
                TransitionManager.go(new Scene(this), trans);
            }

            if (mFullscreen) {
                goFullscreen();
            } else {
                exitFullscreen();
            }
        } finally {
            prefs.edit().putBoolean(mDoFullscreentKey, argNewState).apply();
        }
    }

    private void goFullscreen() {
        Log.d(TAG, "Enter fullscreen mode");
        mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit_white);

        MainActivity activity = ((MainActivity)getContext());
        int topmargin = activity.getFragmentTop() - activity.getSlidingTabsHeight();

        // Main player layout
        CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mLayout.setLayoutParams(layoutParams);
        mLayout.setPadding(0, topmargin, 0, 0);

        int smallMargin = (int)mCenterSquareMargin/2;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mImageContainer.getLayoutParams();
        params.setMargins(smallMargin, mCenterSquareMarginTop, smallMargin, 0);
        params.width = mLayout.getWidth()-2*smallMargin;
        params.height = mLayout.getWidth()-2*smallMargin;
        mImageContainer.setLayoutParams(params);



        mLayout.bringToFront();
        ((MainActivity)getContext()).goFullScreen(mLayout, mBackgroundColor);
    }

    private void exitFullscreen() {
        Log.d(TAG, "Exit fullscreen mode");
        mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_white);

        int topmargin = 0;
        // Main player layout
        CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                sizeLarge);

        topmargin = ((MainActivity)getContext()).getFragmentTop();
        layoutParams.setMargins(0, topmargin, 0, 0);
        mLayout.setLayoutParams(layoutParams);
        mLayout.setPadding(0, mCenterSquareMarginTop, 0, 0);

        if (mLayout.getWidth() > 0) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mImageContainer.getLayoutParams();
            params.setMargins((int) mCenterSquareMargin, 0, (int) mCenterSquareMargin, 0);
            params.width = (int) (mLayout.getWidth() - 2 * mCenterSquareMargin);
            params.height = (int) (mLayout.getWidth() - 2 * mCenterSquareMargin);
            mImageContainer.setLayoutParams(params);
        }

        setPlayerHeight(sizeLarge);
        ((MainActivity)getContext()).exitFullScreen(mLayout);
    }

    public boolean isFullscreen() {
        return mFullscreen;
    }


    private int mLastTouchY;
    private int mInitialTouchY;
    private float startH;
    private int mScrollPointerId;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private int mScrollState = 0;
    private int mTouchSlop;


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mGestureDetector.onTouchEvent(event);

        int xvel;
        int dx = 0; // no horizontal scrolling
        boolean canScrollVertically = true;

        MotionEvent vtev = MotionEvent.obtain(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                mInitialTouchY = this.mLastTouchY = (int)(event.getY() + 0.5F);
                startH = getPlayerHeight();

                this.scrollingChildHelper.startNestedScroll(View.SCROLL_AXIS_VERTICAL);
                Log.v(TAG, "Initiate scroll tracking: y: " + mInitialTouchY + " h: " + startH); // NoI18N
                return true;
            }
            case MotionEvent.ACTION_UP: {
                this.mScrollState = 0;
                this.scrollingChildHelper.stopNestedScroll();
            }
            case MotionEvent.ACTION_MOVE: {
                xvel = MotionEventCompat.findPointerIndex(event, this.mScrollPointerId);
                if(xvel < 0) {
                    Log.e("RecyclerView", "Error processing scroll; pointer index for id " + this.mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }


                int yvel = (int)(MotionEventCompat.getX(event, xvel) + 0.5F);
                int y = (int)(MotionEventCompat.getY(event, xvel) + 0.5F);
                int dy = this.mLastTouchY - y;

                if (dy < ViewConfigurationCompat.getScaledPagingTouchSlop(TopPlayer.this.mViewConfiguration)) {
                    return false;
                }

                if(this.dispatchNestedPreScroll(dx, dy, this.mScrollConsumed, this.mScrollOffset)) {
                    dy -= this.mScrollConsumed[1];
                    vtev.offsetLocation((float)this.mScrollOffset[0], (float)this.mScrollOffset[1]);
                    this.mNestedOffsets[0] += this.mScrollOffset[0];
                    this.mNestedOffsets[1] += this.mScrollOffset[1];
                }

                if(this.mScrollState != 1) {
                    boolean startScroll = false;

                    if(canScrollVertically && Math.abs(dy) > this.mTouchSlop) {
                        if(dy > 0) {
                            dy -= this.mTouchSlop;
                        } else {
                            dy += this.mTouchSlop;
                        }

                        startScroll = true;
                    }

                    if(startScroll) {
                        ViewParent parent = this.getParent();
                        if(parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }

                        this.mScrollState = 1;
                        // this.setScrollState(1); //FIXME
                    }
                }

                if(this.mScrollState == 1) {
                    //this.mLastTouchY = y - this.mScrollOffset[1];
                    //if(this.scrollByInternal(canScrollVertically?dy:0, vtev)) {
                    //    this.getParent().requestDisallowInterceptTouchEvent(true);
                    //}
                }
                break;
            }
        }


        return super.onTouchEvent(event);
    }

    boolean scrollByInternal(int y, MotionEvent ev) {

        final int consumedX = 0;  // always 0
        final int unconsumedX = 0; // always 0

        int unconsumedY = 0;
        int consumedY = 0;

        float height = getPlayerHeight();
        float diff = y;
        float newHeight = height-diff >= sizeSmall ? height-diff : sizeSmall;

        float heightDiffResult = height-newHeight;
        unconsumedY = (int)(diff-heightDiffResult);
        consumedY = unconsumedY - (int)diff;



        if(this.dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, this.mScrollOffset)) {
            this.mLastTouchY -= this.mScrollOffset[1];
            if(ev != null) {
                ev.offsetLocation((float)this.mScrollOffset[0], (float)this.mScrollOffset[1]);
            }

            this.mNestedOffsets[0] += this.mScrollOffset[0];
            this.mNestedOffsets[1] += this.mScrollOffset[1];
        } else if(ViewCompat.getOverScrollMode(this) != ViewCompat.OVER_SCROLL_NEVER) {
            if(ev != null) {
                //this.pullGlows(ev.getX(), (float)unconsumedX, ev.getY(), (float)unconsumedY);
            }

            //this.considerReleasingGlowsOnScroll(x, y);
        }

        boolean doScroll = consumedX != 0 || consumedY != 0;

        if(doScroll) {
            //this.dispatchOnScrolled(consumedX, consumedY);
            setPlayerHeight(newHeight);
            Log.v(TAG, "Set height: H: " + height + " diff: " + diff + " newH: " + newHeight); // NoI18N
        }

        if(!this.awakenScrollBars()) {
            this.invalidate();
        }

        return doScroll;
    }

    class TopPLayerScrollGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        public TopPLayerScrollGestureListener() {
        }

        @Override
        public  boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(DEBUG_TAG, "onScroll: e1Y: " + e1.getY() + " e2Y: " + e2.getY());

            float y1 = e1.getY();
            float y2 = e2.getY();

            float diffY = Math.abs(y1 - y2);
            if (diffY > ViewConfigurationCompat.getScaledPagingTouchSlop(TopPlayer.this.mViewConfiguration)) {
                Log.d(DEBUG_TAG, "onScroll: dispatchScroll: diffY: " + diffY);
                TopPlayer.this.dispatchNestedScroll(0, 0, (int) distanceX, (int) distanceY, new int[2]);
                return true;
            }

            Log.d(DEBUG_TAG, "onScroll: ignore: diffY: " + diffY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
            //mRecyclerView.fling((int)velocityX*0, (int)-velocityY);
            TopPlayer.this.dispatchNestedFling(velocityX, -velocityY, false);
            return true;
        }
    }

    // Nested scrolling
    public void setNestedScrollingEnabled(boolean enabled) {
        scrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    public boolean isNestedScrollingEnabled() {
        return scrollingChildHelper.isNestedScrollingEnabled();
    }

    public boolean startNestedScroll(int axes) {
        return scrollingChildHelper.startNestedScroll(axes);
    }

    public void stopNestedScroll() {
        scrollingChildHelper.stopNestedScroll();
    }

    public boolean hasNestedScrollingParent() {
        return scrollingChildHelper.hasNestedScrollingParent();
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {

        return scrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed, offsetInWindow);
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return scrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return scrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return scrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }


    // ScrollingView

    // Compute the horizontal extent of the horizontal scrollbar's thumb within the horizontal range.
    public int computeHorizontalScrollOffset() {
        return 0;// this.mLayout.canScrollHorizontally()?this.mLayout.computeHorizontalScrollOffset(this.mState):0;
    }

    // Compute the horizontal offset of the horizontal scrollbar's thumb within the horizontal range.
    public int computeHorizontalScrollExtent() {
        return 0;// this.mLayout.canScrollHorizontally()?this.mLayout.computeHorizontalScrollExtent(this.mState):0;
    }

    // Compute the horizontal range that the horizontal scrollbar represents.
    public int computeHorizontalScrollRange() {
        return 0;// this.mLayout.canScrollHorizontally()?this.mLayout.computeHorizontalScrollRange(this.mState):0;
    }

    // Compute the vertical extent of the vertical scrollbar's thumb within the vertical range.
    public int computeVerticalScrollOffset() {
        return 0;// this.mLayout.canScrollVertically()?this.mLayout.computeVerticalScrollOffset(this.mState):0;
    }

    // Compute the vertical offset of the vertical scrollbar's thumb within the horizontal range.
    public int computeVerticalScrollExtent() {
        return 0;// this.mLayout.canScrollVertically()?this.mLayout.computeVerticalScrollExtent(this.mState):0;
    }

    // Compute the vertical range that the vertical scrollbar represents.
    public int computeVerticalScrollRange() {
        return 0;// this.mLayout.canScrollVertically()?this.mLayout.computeVerticalScrollRange(this.mState):0;
    }


}
