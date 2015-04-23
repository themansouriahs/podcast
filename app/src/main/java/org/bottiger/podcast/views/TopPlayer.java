package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.UIUtils;

import static org.bottiger.podcast.views.PlayerButtonView.StaticButtonColor;

/**
 * Created by apl on 30-09-2014.
 */
public class TopPlayer extends RelativeLayout implements PaletteListener {

    private FeedItem mEpisodeId;

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

    private PlayerRelativeLayout mPlayerControlsLinearLayout;

    private RelativeLayout mPlayerButtons;
    private int mPlayerButtonsHeight = -1;

    private View mEpisodeText;
    private View mEpisodeInfo;
    private View mPhoto;
    private PlayPauseImageView mPlayPauseButton;
    private View mSeekbarContainer;

    private PlayerSeekbar mSeekbar;

    private View mForwardButton;
    private View mBackButton;
    private View mDownloadButton;
    private View mQueueButton;
    private View mFavoriteButton;

    private PlayerLayoutParameter mSmallLayout = new PlayerLayoutParameter();
    private PlayerLayoutParameter mLargeLayout = new PlayerLayoutParameter();

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
        mActivity = (Activity)argContext;

        sizeSmall = mActivity.getResources().getDimensionPixelSize(R.dimen.top_player_size_minimum);
        sizeMedium = mActivity.getResources().getDimensionPixelSize(R.dimen.top_player_size_medium);
        sizeLarge = mActivity.getResources().getDimensionPixelSize(R.dimen.top_player_size_maximum);

        screenHeight = sizeLarge;

        sizeShrinkBuffer = (int)UIUtils.convertDpToPixel(100, mActivity);

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

    public void setRecyclerView(@NonNull FixedRecyclerView argRecyclerView) {
        mRecyclerView = argRecyclerView;
    }

    public int maxTrans() {
        return sizeSmall-sizeLarge;
    }

    @Override
         public boolean onTouchEvent(MotionEvent event) {
        return mRecyclerView.onTouchEvent(event);
    }

    @Override
    protected void onFinishInflate () {
        mPlayerControlsLinearLayout = (PlayerRelativeLayout)findViewById(R.id.expanded_controls);

        mPhoto = findViewById(R.id.session_photo);

        mPlayPauseButton = (PlayPauseImageView) findViewById(R.id.play_pause_button);
        mEpisodeText = findViewById(R.id.episode_title);
        mEpisodeInfo = findViewById(R.id.episode_info);
        mSeekbarContainer = findViewById(R.id.player_progress);
        mForwardButton = findViewById(R.id.fast_forward_button);
        mBackButton = findViewById(R.id.previous);
        mDownloadButton = findViewById(R.id.download);
        mQueueButton = findViewById(R.id.queue);
        mFavoriteButton = findViewById(R.id.bookmark);

        mSeekbar = (PlayerSeekbar) findViewById(R.id.player_progress);
        mPlayerButtons = (RelativeLayout) findViewById(R.id.player_buttons);

        mPlayPauseLargeSize = mPlayPauseButton.getLayoutParams().height;

        //PaletteCache.generate(mEpisodeId.getImageURL(mActivity), mActivity, mSeekbar);

        mLargeLayout.SeekBarLeftMargin = 0;
        mLargeLayout.PlayPauseSize = mPlayPauseLargeSize;
        mLargeLayout.PlayPauseBottomMargin = ((RelativeLayout.LayoutParams)mPlayPauseButton.getLayoutParams()).bottomMargin;
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
        mSeekbarContainer.setTranslationY(transYControl);
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
        mSeekbarContainer.setTranslationY(transYControl);
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
        return setGenericVisibility(mSeekbarContainer, sizeMedium, mSeekbarFadeDistancePx, argTopPlayerHeight);
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

    public synchronized void setEpisodeId(FeedItem argEpisode) {
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


}
