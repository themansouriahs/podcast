package org.bottiger.podcast.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.drawables.PlayPauseDrawable;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by aplb on 17-10-2015.
 */
public class PlayPauseView extends View {

    private static final Property<PlayPauseView, Integer> COLOR =
            new Property<PlayPauseView, Integer>(Integer.class, "color") {
                @Override
                public Integer get(PlayPauseView v) {
                    return v.getColor();
                }

                @Override
                public void set(PlayPauseView v, Integer value) {
                    v.setColor(value);
                }
            };

    private static final long PLAY_PAUSE_ANIMATION_DURATION = 200;

    private ReentrantLock mLock = new ReentrantLock();

    private final PlayPauseDrawable mDrawable;
    private final Paint mPaint = new Paint();
    private final Paint mPaintBackground = new Paint();
    private int mPauseBackgroundColor;
    private int mPlayBackgroundColor;

    private AnimatorSet mAnimatorSet;
    private int mBackgroundColor;

    private @ColorRes int color1 = R.color.colorPrimaryDark;
    private @ColorRes int color2 = R.color.colorPrimaryDark;

    public PlayPauseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mBackgroundColor = ContextCompat.getColor(context, color1);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);

        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setStyle(Paint.Style.FILL);

        mDrawable = new PlayPauseDrawable(context);
        mDrawable.setCallback(this);

        mPauseBackgroundColor = ContextCompat.getColor(context, color1);
        mPlayBackgroundColor = ContextCompat.getColor(context, color2);
    }

    public void setColor(int color) {
        mBackgroundColor = color;
        mPlayBackgroundColor = color;
        mPauseBackgroundColor = color;
        invalidate();
    }

    private int getColor() {
        return mBackgroundColor;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(mBackgroundColor);
        final float radius = Math.min(getWidth(), getHeight()) / 2f;
        final float cx = getWidth()/2f;
        final float cy = getHeight()/2f;
        canvas.drawCircle(cx, cy, radius, mPaint);

        //float transY = (getHeight()-getWidth())/2f;
        //canvas.translate(0, transY);
        mDrawable.draw(canvas);
    }


    public void setIconColor(@ColorInt int argColor) {
        mDrawable.setTint(argColor);
        invalidate();
    }


    public void setState(@PlayPauseDrawable.IconState int argState) {
        mLock.lock();
        try {
            if (argState == mDrawable.getIconState())
                return;

            if (mAnimatorSet != null && mAnimatorSet.isRunning())
                return;

        } finally {
            mLock.unlock();
        }
    }

    public void animateChangeFrom(@PlayPauseDrawable.IconState int argFromState) {
        mLock.lock();
        try {
            if (mAnimatorSet != null) {
                mAnimatorSet.cancel();
            }

            mAnimatorSet = new AnimatorSet();
            final boolean isPlay = mDrawable.isPlaying();
            final ObjectAnimator colorAnim = ObjectAnimator.ofInt(this, COLOR, isPlay ? mPauseBackgroundColor : mPlayBackgroundColor);
            colorAnim.setEvaluator(new ArgbEvaluator());
            final Animator pausePlayAnim = argFromState == PlayPauseDrawable.IS_PLAYING ? mDrawable.getToPauseAnimator(): mDrawable.getToPlayAnimator();
            mAnimatorSet.setInterpolator(new DecelerateInterpolator());
            mAnimatorSet.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
            mAnimatorSet.playTogether(colorAnim, pausePlayAnim);
            mAnimatorSet.start();
        } finally {
            mLock.unlock();
        }
    }

    public boolean IsDisplayingPlayIcon() {
        return !mDrawable.isPlaying();
    }
}
