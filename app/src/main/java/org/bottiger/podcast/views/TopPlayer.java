package org.bottiger.podcast.views;

import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.utils.UIUtils;

/**
 * Created by apl on 30-09-2014.
 */
public class TopPlayer extends RelativeLayout {

    private enum PlayerLayout { SMALL, MEDIUM, LARGE }
    private PlayerLayout mPlayerLayout = PlayerLayout.LARGE;

    private int mPlayPauseLargeSize = -1;

    private Context mContext;
    private FixedRecyclerView mRecyclerView;

    public static int sizeSmall                 =   -1;
    public static int sizeMedium                =   -1;
    public static int sizeLarge                 =   -1;
    public static int sizeActionbar             =   -1;

    private boolean mSeekbarVisible             =   true;
    private int mSeekbarDeadzone                =   20; // dp
    private int mSeekbarFadeDistance            =   20; // dp
    private int mTextInfoFadeDistance           =   100; // dp
    private int mTextFadeDistance               =   20; // dp

    private int mSeekbarDeadzonePx;
    private int mSeekbarFadeDistancePx;
    private int mTextFadeDistancePx;
    private int mTextInfoFadeDistancePx;

    private boolean mCanScrollUp = true;

    private PlayerLinearLayout mPlayerControlsLinearLayout;

    private LinearLayout mPlayerButtons;
    private int mPlayerButtonsHeight = -1;

    private View mEpisodeText;
    private View mEpisodeInfo;
    private View mPhoto;
    private View mPlayPauseButton;
    private View mSeekbar;

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
        mContext = argContext;

        sizeSmall = mContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_minimum);
        sizeMedium = mContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_medium);
        sizeLarge = mContext.getResources().getDimensionPixelSize(R.dimen.top_player_max_offset);
        //sizeLarge = 1080; // 1080

        mSeekbarDeadzonePx        = (int)UIUtils.convertDpToPixel(mSeekbarDeadzone, mContext);
        mSeekbarFadeDistancePx    = (int)UIUtils.convertDpToPixel(mSeekbarFadeDistance, mContext);
        mTextFadeDistancePx       = (int)UIUtils.convertDpToPixel(mTextFadeDistance, mContext);
        mTextInfoFadeDistancePx   = (int)UIUtils.convertDpToPixel(mTextInfoFadeDistance, mContext);

        sizeActionbar = mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
        }
    }

    public void setRecyclerView(@NonNull FixedRecyclerView argRecyclerView) {
        mRecyclerView = argRecyclerView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mRecyclerView.onTouchEvent(event);
    }

    @Override
    protected void onFinishInflate () {
        mPlayerControlsLinearLayout = (PlayerLinearLayout)findViewById(R.id.expanded_controls);

        mPhoto = findViewById(R.id.session_photo);

        mPlayPauseButton = findViewById(R.id.play_pause_button);
        mEpisodeText = findViewById(R.id.episode_title);
        mEpisodeInfo = findViewById(R.id.episode_info);
        mSeekbar = findViewById(R.id.player_progress);
        mBackButton = findViewById(R.id.previous);
        mDownloadButton = findViewById(R.id.download);
        mQueueButton = findViewById(R.id.queue);
        mFavoriteButton = findViewById(R.id.bookmark);

        mPlayerButtons = (LinearLayout) findViewById(R.id.player_buttons);

        mPlayPauseLargeSize = mPlayPauseButton.getLayoutParams().height;

        PaletteObservable.registerListener((PlayerSeekbar)mSeekbar);

        mLargeLayout.SeekBarLeftMargin = 0;
        mLargeLayout.PlayPauseSize = mPlayPauseLargeSize;
        mLargeLayout.PlayPauseBottomMargin = ((RelativeLayout.LayoutParams)mPlayPauseButton.getLayoutParams()).bottomMargin;
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    // returns actual visible height
    public float setPlayerHeight(float argScreenHeight, float argOffset) {
        if (!validateState()) {
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

        int offset = -1;
        if (argScreenHeight <= sizeMedium) {

            if (mPlayerButtonsHeight < 0) {
                mPlayerButtonsHeight = mPlayerButtons.getHeight()-(int)UIUtils.convertPixelsToDp(50, mContext);

                mSmallLayout.SeekBarLeftMargin = mPlayPauseButton.getLeft() + mSeekbar.getHeight();
                mSmallLayout.PlayPauseSize = mSeekbar.getHeight();
                mSmallLayout.PlayPauseBottomMargin = 0;
            }
            offset = sizeMedium - (int)maxScreenHeight;
            int buttonOffset = offset > mPlayerButtonsHeight ? mPlayerButtonsHeight : offset;
            mPlayerControlsLinearLayout.setTranslationY(buttonOffset);
            mPlayPauseButton.setTranslationY(buttonOffset);
        }

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
        return setGenericVisibility(mSeekbar, sizeMedium, mSeekbarFadeDistancePx, argTopPlayerHeight);
    }

    public float setGenericVisibility(@NonNull View argView, int argVisibleHeight, int argFadeDistance, float argTopPlayerHeight) {
        Log.d("Top", "setGenericVisibility: h: " + argTopPlayerHeight);

        int VisibleHeightPx = argVisibleHeight;
        int InvisibleHeightPx = argVisibleHeight-argFadeDistance;

        if (argTopPlayerHeight > VisibleHeightPx)
            return 1f;

        if (argTopPlayerHeight < InvisibleHeightPx || argTopPlayerHeight == sizeSmall)
            return 0f;

        float thressholdDiff = VisibleHeightPx - InvisibleHeightPx;
        float DistanceFromVisible = argTopPlayerHeight-InvisibleHeightPx;

        float VisibilityFraction = DistanceFromVisible / thressholdDiff;

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
