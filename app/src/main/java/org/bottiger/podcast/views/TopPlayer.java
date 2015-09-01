package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.UIUtils;

import static org.bottiger.podcast.views.PlayerButtonView.StaticButtonColor;

/**
 * Created by apl on 30-09-2014.
 */
public class TopPlayer extends RelativeLayout implements PaletteListener {

    private static final String TAG = "TopPlayer";

    private IEpisode mEpisodeId;

    private enum PlayerLayout { SMALL, MEDIUM, LARGE }
    private PlayerLayout mPlayerLayout = PlayerLayout.LARGE;

    private int mPlayPauseLargeSize = -1;

    private Activity mActivity;
    private FixedRecyclerView mRecyclerView;

    public static int sizeSmall                 =   -1;
    public static int sizeMedium                =   -1;
    public static int sizeLarge                 =   -1;
    public static int sizeActionbar             =   -1;

    public static int sizeStartShrink           =   -1;
    public static int sizeShrinkBuffer           =   -1;

    private boolean mSeekbarVisible             =   true;
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

    private RelativeLayout mLayout;
    private ImageButton mExpandEpisode;
    private View mGradient;
    private View mEpisodeText;
    private View mEpisodeInfo;
    private SimpleDraweeView mPhoto;
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

    private PlayerLayoutParameter mSmallLayout = new PlayerLayoutParameter();
    private PlayerLayoutParameter mLargeLayout = new PlayerLayoutParameter();

    private SharedPreferences prefs;

    private class PlayerLayoutParameter {
        public int SeekBarLeftMargin;
        public int PlayPauseSize;
        public int PlayPauseBottomMargin;
    }

    public TopPlayer(Context context) {
        super(context);
        init(context);
    }

    public TopPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TopPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public TopPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(@NonNull Context argContext) {

        int screenHeight = 1000;

        if (isInEditMode()) {
            return;
        }

        mActivity = (Activity) argContext;

        prefs = PreferenceManager.getDefaultSharedPreferences(argContext.getApplicationContext());
        mDoDisplayTextKey = mActivity.getResources().getString(R.string.pref_top_player_text_expanded_key);
        mDoDisplayText = prefs.getBoolean(mDoDisplayTextKey, mDoDisplayText);

        mDoFullscreentKey = mActivity.getResources().getString(R.string.pref_top_player_fullscreen_key);
        mFullscreen = prefs.getBoolean(mDoFullscreentKey, mFullscreen);


        sizeShrinkBuffer = (int) UIUtils.convertDpToPixel(100, mActivity);
        screenHeight = UIUtils.getScreenHeight(mActivity);

        sizeSmall = mActivity.getResources().getDimensionPixelSize(R.dimen.top_player_size_minimum);
        sizeMedium = mActivity.getResources().getDimensionPixelSize(R.dimen.top_player_size_medium);
        sizeLarge = screenHeight- mActivity.getResources().getDimensionPixelSize(R.dimen.top_player_size_maximum_bottom);

        screenHeight = sizeLarge;

        sizeStartShrink = sizeSmall+sizeShrinkBuffer;
        //sizeLarge = 1080; // 1080

        mSeekbarDeadzonePx        = (int)UIUtils.convertDpToPixel(mSeekbarDeadzone, mActivity);
        mSeekbarFadeDistancePx    = (int)UIUtils.convertDpToPixel(mSeekbarFadeDistance, mActivity);
        mTextFadeDistancePx       = (int)UIUtils.convertDpToPixel(mTextFadeDistance, mActivity);
        mTextInfoFadeDistancePx   = (int)UIUtils.convertDpToPixel(mTextInfoFadeDistance, mActivity);

        sizeActionbar = mActivity.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLayout = (RelativeLayout) findViewById(R.id.session_photo_container);

        mPlayerControlsLinearLayout = (PlayerRelativeLayout)findViewById(R.id.expanded_controls);

        mPhoto = (SimpleDraweeView) findViewById(R.id.session_photo);

        mPlayPauseButton = (PlayPauseImageView) findViewById(R.id.play_pause_button);
        mExpandEpisode = (ImageButton)findViewById(R.id.episode_expand);
        mEpisodeText = findViewById(R.id.episode_title);
        mEpisodeInfo = findViewById(R.id.episode_info);
        mImageContainer = findViewById(R.id.player_progress);
        mForwardButton = findViewById(R.id.fast_forward_button);
        mBackButton = findViewById(R.id.rewind_button);
        mFullscreenButton = (PlayerButtonView) findViewById(R.id.fullscreen_button);
        mDownloadButton = findViewById(R.id.download);
        mQueueButton = findViewById(R.id.queue);
        mFavoriteButton = findViewById(R.id.bookmark);
        mGradient = findViewById(R.id.top_gradient_inner);
        mSleepButton = (PlayerButtonView) findViewById(R.id.sleep_button);

        mSeekbar = (PlayerSeekbar) findViewById(R.id.player_progress);
        mPlayerButtons = (RelativeLayout) findViewById(R.id.player_buttons);

        mPlayPauseLargeSize = mPlayPauseButton.getLayoutParams().height;

        //PaletteCache.generate(mEpisodeId.getArtwork(mActivity), mActivity, mSeekbar);

        mLargeLayout.SeekBarLeftMargin = 0;
        mLargeLayout.PlayPauseSize = mPlayPauseLargeSize;
        mLargeLayout.PlayPauseBottomMargin = ((RelativeLayout.LayoutParams)mPlayPauseButton.getLayoutParams()).bottomMargin;

        if (!mDoDisplayText) {
            mExpandEpisode.setImageResource(R.drawable.ic_expand_more_white);
            mEpisodeText.setVisibility(GONE);
            mGradient.setVisibility(GONE);
            mEpisodeInfo.setVisibility(GONE);
        }

        // Give image a fixed height

        //int width = mPhoto.getWidth();
        //RelativeLayout.LayoutParams params = (LayoutParams) mPhoto.getLayoutParams();
        //params.width = width;
        //params.height = width;
        //mPhoto.setLayoutParams(params);
        //mPhoto.getLayoutParams().height = mPhoto.getLayoutParams().width;

        mSleepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sleepButtonPressed();
            }
        });

        setFullscreenState(mFullscreen);

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
                    setFullscreenState(mFullscreen);
                } finally {
                    prefs.edit().putBoolean(mDoFullscreentKey, mFullscreen).commit();
                }
            }
        });
    }

    private void sleepButtonPressed() {
        try {
            PlayerService ps = PlayerService.getInstance();
            int minutes = 30;
            int onemin = 1000 * 60;
            ps.FaceOutAndStop(onemin*minutes);
            String toast = getResources().getQuantityString(R.plurals.player_sleep, minutes, minutes);
            Toast.makeText(mActivity, toast, Toast.LENGTH_LONG).show();
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

    // returns actual visible height
    public float setPlayerHeight(float argScreenHeight) {

        if (mFullscreen)
            return getHeight();

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
        /*
        mPlayerControlsLinearLayout.setTranslationY(transYControl);
        mPlayPauseButton.setTranslationY(transYControl);
        mImageContainer.setTranslationY(transYControl);
        mForwardButton.setTranslationY(transYControl);
        */

        //setTranslationY(transYControl);
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = (int)screenHeight;
        setLayoutParams(lp);


        if (System.currentTimeMillis() > 0) {
            return -1;
        }

        Log.d("setTranslationXYZ", "argScreenHeight: " +  argScreenHeight);

        float maxScreenHeight = argScreenHeight > sizeLarge ? sizeLarge : argScreenHeight;

        Log.d("setTranslationXYZ", "maxScreenHeight: " +  maxScreenHeight);

        float transY;
        float minMaxScreenHeight = maxScreenHeight;
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
        return sizeSmall == getHeight()+getTranslationY();
    }

    public int getMinimumSize() {
        return sizeSmall;
    }

    public int getMaximumSize() {
        return sizeLarge;
    }

    public boolean isMaximumSize() {
        Log.d("MaximumSize", "Trans: " + getTranslationY());
        return mPlayerLayout == PlayerLayout.LARGE && getTranslationY() >= 0; // FIXME: refine
    }

    private void translatePhotoY(@NonNull View argImageView , float amount) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            argImageView.setTranslationY(-(amount/2));
        } else {
            //argImageView.setTranslationY(-amount);
        }
    }

    public synchronized void setEpisodeId(IEpisode argEpisode) {
        this.mEpisodeId = argEpisode;
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        setBackgroundColor(StaticButtonColor(mActivity, argChangedPalette));
        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return mPlayPauseButton.getPaletteUrl();
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        private FixedRecyclerView mRecyclerView;

        public MyGestureListener(FixedRecyclerView argRecyclerView) {
            mRecyclerView = argRecyclerView;
        }

        @Override
        public  boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(DEBUG_TAG,"onDown: " + e1.toString());
            mRecyclerView.scrollBy((int)distanceX*0, (int)distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
            mRecyclerView.fling((int)velocityX*0, (int)-velocityY);
            return true;
        }
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

    private void setFullscreenState(boolean argNewState) {

        if (mFullscreen) {
            goFullscreen();
        } else {
            exitFullscreen();
        }
    }

    private LinearLayout.LayoutParams paramCache;

    private void goFullscreen() {
        Log.d(TAG, "Enter fullscreen mode");
        mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit_white);

        //mPhoto.getLayoutParams().height = mPhoto.getWidth();

        // Main player layout
        LinearLayout.LayoutParams paramCache = (LinearLayout.LayoutParams) mLayout.getLayoutParams();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        mLayout.setLayoutParams(layoutParams);

    }

    private void exitFullscreen() {
        Log.d(TAG, "Exit fullscreen mode");
        mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_white);

        // Main player layout
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                sizeLarge);
        mLayout.setLayoutParams(layoutParams);
    }


}
