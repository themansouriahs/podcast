package org.bottiger.podcast.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;

/**
 * Created by apl on 30-09-2014.
 */
public class TopPlayer extends RelativeLayout {

    private enum PlayerLayout { SMALL, MEDIUM, LARGE }
    private PlayerLayout mPlayerLayout = PlayerLayout.LARGE;

    private boolean SEEKBAR_FULL_WIDTH = true;
    private int mPlayPauseLargeSize = -1;

    private Context mContext;

    private ViewGroup.LayoutParams mSeekbarParams;
    private ViewGroup.LayoutParams mPlayPauseParams;

    private Scene mLargePlayer;
    private Scene mMediumPlayer;
    private Scene mButtons;

    private AnimatorSet mAnimationSet;
    private ValueAnimator mScaleButtonSize;
    private ValueAnimator mScaleSeekbarSize;
    private ValueAnimator mTranslateButtonSize;

    private TransitionManager mTransitionManager;
    private Transition mChangeBoundsLargeToMedium;
    private TimeInterpolator mInterpolator = new DecelerateInterpolator();
    private long mAnimationStartTime = -1;
    private long mAnimationDuration = 100;

    public static int sizeSmall   = -1;
    public static int sizeMedium  = 1000;
    public static int sizeLarge   = -1;
    public static int sizeActionbar   = -1;

    private static int sPlayPauseMarginSmall = 300;
    private static int sPlayPauseMarginLarge = 660;

    private PlayerLayout mCurrentLayout = PlayerLayout.LARGE;
    private boolean mCanScrollUp = true;

    private int mSeekbarLeft = -1;
    private int mSeekbarOffset = -1;

    private PlayerLinearLayout mPlayerControlsLinearLayout;

    private LinearLayout mPlayerButtons;
    private int mPlayerButtonsHeight = -1;

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
        public int LayoutRule;
        public int LayoutVerb;
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

    public TopPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(@NonNull Context argContext) {
        mContext = argContext;

        mAnimationSet = new AnimatorSet();

        sizeSmall = mContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_minimum);
        sizeLarge = mContext.getResources().getDimensionPixelSize(R.dimen.top_player_max_offset);
        //sizeLarge = 1080; // 1080

        sizeActionbar = mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

        mTransitionManager = new TransitionManager();

        setClipToOutline(true);
    }


    @Override
    protected void onFinishInflate () {
        mPlayerControlsLinearLayout = (PlayerLinearLayout)findViewById(R.id.expanded_controls);

        mPlayPauseButton = findViewById(R.id.play_pause_button);
        mSeekbar = findViewById(R.id.player_progress);
        mBackButton = findViewById(R.id.previous);
        mDownloadButton = findViewById(R.id.download);
        mQueueButton = findViewById(R.id.queue);
        mFavoriteButton = findViewById(R.id.bookmark);

        mPlayerButtons = (LinearLayout) findViewById(R.id.player_buttons);

        mPlayPauseLargeSize = mPlayPauseButton.getLayoutParams().height;
        mSeekbarParams = mSeekbar.getLayoutParams();
        mPlayPauseParams = mPlayPauseButton.getLayoutParams();

        mLargePlayer = Scene.getSceneForLayout(mPlayerControlsLinearLayout, R.layout.extended_player, mContext);
        mMediumPlayer = Scene.getSceneForLayout(mPlayerControlsLinearLayout, R.layout.extended_player_medium, mContext);
        mButtons = Scene.getSceneForLayout(mPlayerButtons, R.layout.extended_player_buttons, mContext);

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
        mCanScrollUp = maxScreenHeight > sizeSmall;

        Log.d("TopPlayerInput", "(canScrollUp: " + mCanScrollUp + ")maxScreenHeight -> " + maxScreenHeight + "ScreenOffset -> " + argOffset);

        int offset = -1;
        if (argScreenHeight <= sizeMedium) {
            if (mPlayerButtonsHeight < 0) {
                mPlayerButtonsHeight = mPlayerButtons.getHeight()-50;

                mSmallLayout.SeekBarLeftMargin = mPlayPauseButton.getLeft() + mSeekbar.getHeight();
                mSmallLayout.PlayPauseSize = mSeekbar.getHeight();
                mSmallLayout.PlayPauseBottomMargin = 0;
            }
            offset = sizeMedium - (int)maxScreenHeight;
            int buttonOffset = offset > mPlayerButtonsHeight ? mPlayerButtonsHeight : offset;
            mPlayerControlsLinearLayout.setTranslationY(buttonOffset);
            mPlayPauseButton.setTranslationY(buttonOffset);
        }

        /*
        if (!mCanScrollUp) {


            getLayoutParams().height = sizeSmall;

            mPlayPauseButton.setTranslationY(-sizeSmall+sizeActionbar/2);
            //return sizeSmall;
        } else {
            getLayoutParams().height = sizeLarge;
        }*/

        String size = "large";
        if (maxScreenHeight >= sizeSmall) {
            getLayoutParams().height = sizeLarge;

            transY = minMaxScreenHeight-sizeLarge; //argOffset;
            Log.d("setTranslationXYZ", "minMaxScreenHeight: " +  minMaxScreenHeight);

            setTranslationY(transY);
        } else {
            size = "small";
            getLayoutParams().height = sizeSmall;

            mPlayPauseButton.setTranslationY(-sizeSmall+sizeActionbar/2);

            transY = -(getHeight()-sizeSmall);
            setTranslationY(transY);
            minMaxScreenHeight = sizeSmall;
        }

        Log.d("setTranslationXYZ", size + ": " +  getLayoutParams().height + " trans: " + transY);

        if (offset > 0) { // offset == mPlayerButtonsHeight &&
            transformLayout((SeekBar) mSeekbar, (PlayPauseImageView) mPlayPauseButton, PlayerLayout.SMALL);
        } else {
            transformLayout((SeekBar) mSeekbar, (PlayPauseImageView) mPlayPauseButton, PlayerLayout.LARGE);
        }

        Log.d("TopPlayerInputk", "layoutparams =>" + getLayoutParams().height + "Translation =>" + transY + " minMaxHeight =>" + minMaxScreenHeight);
        return minMaxScreenHeight; // return newVisibleHeight
    }


    public boolean isSmall() {
        return mPlayerLayout == PlayerLayout.SMALL;
    }

    public boolean isLarge() {
        return mPlayerLayout == PlayerLayout.LARGE;
    }

    private boolean validateState() {
        if (sizeSmall < 0 || sizeMedium < 0 || sizeLarge < 0) {
            Log.d("TopPlayer", "Layout sizes needs to be defined");
            return false;
        }
        return true;
    }

    public boolean isMinimumSize() {
        //return !mCanScrollUp;
        return sizeSmall == getHeight()+getTranslationY();
    }

    public boolean isMaximumSize() {
        Log.d("MaximumSize", "Trans: " + getTranslationY());
        return mPlayerLayout == PlayerLayout.LARGE && getTranslationY() >= 0; // FIXME: refine
    }

    private void transformLayout(@NonNull SeekBar argSeekbar, @NonNull PlayPauseImageView argPlayPause, PlayerLayout argTargetLayout) {

        if (mPlayerLayout == argTargetLayout) {
            return;
        }

        Log.d("LayoutTransform", "From: " + mPlayerLayout + " to: " + argTargetLayout);

        final PlayerLayoutParameter startValue, endValues;

        if (argTargetLayout == PlayerLayout.SMALL) {
            mPlayerLayout = PlayerLayout.SMALL;
            startValue = mLargeLayout;
            endValues = mSmallLayout;
        } else {
            mPlayerLayout = PlayerLayout.LARGE;
            startValue = mSmallLayout;
            endValues = mLargeLayout;
        }

        int height = argSeekbar.getHeight(); // min value

        final RelativeLayout.LayoutParams playPauseParams = (RelativeLayout.LayoutParams) mPlayPauseButton.getLayoutParams();
        final FrameLayout.LayoutParams seekbarParams = (FrameLayout.LayoutParams) mSeekbar.getLayoutParams();

        playPauseParams.width = endValues.PlayPauseSize;
        playPauseParams.height = endValues.PlayPauseSize;

        if (mPlayerLayout == PlayerLayout.SMALL) {
            playPauseParams.topMargin = 660;
        } else {
            playPauseParams.topMargin = 300;
        }

        seekbarParams.leftMargin = endValues.SeekBarLeftMargin;

        /*
        mTransitionManager.beginDelayedTransition(this);

        mPlayPauseButton.setLayoutParams(playPauseParams);
        mSeekbar.setLayoutParams(seekbarParams);
        */
        Log.d("Interp", "gogog");
        Animation a = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                playPauseParams.width = (int)((endValues.PlayPauseSize-startValue.PlayPauseSize) * interpolatedTime) + startValue.PlayPauseSize;
                playPauseParams.height = (int)((endValues.PlayPauseSize-startValue.PlayPauseSize) * interpolatedTime) + startValue.PlayPauseSize;

                int ifinal = -1;
                int newTopMargin = -1;
                if (mPlayerLayout == PlayerLayout.SMALL) {
                    ifinal = sPlayPauseMarginLarge;
                    newTopMargin = (int)((sPlayPauseMarginLarge-sPlayPauseMarginSmall) * interpolatedTime) + sPlayPauseMarginSmall;
                } else {
                    ifinal = sPlayPauseMarginSmall;
                    newTopMargin = (int)(sPlayPauseMarginLarge + (sPlayPauseMarginSmall-sPlayPauseMarginLarge) * interpolatedTime - (1-interpolatedTime)*0.5*playPauseParams.height);
                    //newTopMargin = sPlayPauseMarginLarge;
                }

                playPauseParams.topMargin = newTopMargin;

                seekbarParams.leftMargin = (int)(endValues.SeekBarLeftMargin * interpolatedTime);

                Log.d("InterpSize", "w ->" + playPauseParams.width);
                Log.d("Interp", "i ->" + interpolatedTime + " margin -> " + playPauseParams.topMargin + "(final)" + ifinal + " cal:" + newTopMargin);
                mPlayPauseButton.setLayoutParams(playPauseParams);
                mSeekbar.setLayoutParams(seekbarParams);
                //requestLayout();
            }
        };

        //ObjectAnimator anim = ObjectAnimator.ofFloat(mSeekbar, "alpha", 0f, 1f);
        a.setDuration(100);
        //a.start();
        startAnimation(a);
    }

}
