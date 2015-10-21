package org.bottiger.podcast.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.drawables.PlayPauseDrawable;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by aplb on 17-10-2015.
 */
public class PlayPauseView extends FrameLayout {

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
    private int mPauseBackgroundColor;
    private int mPlayBackgroundColor;

    private AnimatorSet mAnimatorSet;
    private int mBackgroundColor;
    private int mWidth;
    private int mHeight;

    private @ColorRes int color1 = R.color.colorPrimary;
    private @ColorRes int color2 = R.color.colorPrimary;

    public PlayPauseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mBackgroundColor = ContextCompat.getColor(context, color1);//getResources().getColor(R.color.purple);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mDrawable = new PlayPauseDrawable(context);
        //mDrawable.setTint(Color.WHITE);
        mDrawable.setCallback(this);

        mPauseBackgroundColor = ContextCompat.getColor(context, color1);
        mPlayBackgroundColor = ContextCompat.getColor(context, color2);

        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    UIUtils.removeOnGlobalLayoutListener(PlayPauseView.this, this);
                    setViewSize(getWidth(), getHeight());
                }
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setViewSize(w, h);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            setClipToOutline(true);
        }
    }

    private void setColor(int color) {
        mBackgroundColor = color;
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
        final float radius = Math.min(mWidth, mHeight) / 2f;
        super.onDraw(canvas);
        mPaint.setColor(mBackgroundColor);
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, radius, mPaint);
        mDrawable.draw(canvas);
    }

    public void setIconColor(@ColorInt int argColor) {
        mDrawable.setTint(argColor);
        invalidate();
    }

    public void setBackgroundColor(@ColorInt int argColor) {
        mBackgroundColor = argColor;
        mPauseBackgroundColor = argColor;
        mPlayBackgroundColor = argColor;
        invalidate();
    }

    public void setState(@PlayPauseDrawable.IconState int argState) {
        mLock.lock();
        try {
            if (argState == mDrawable.getIconState())
                return;

            if (mAnimatorSet != null && mAnimatorSet.isRunning())
                return;

            mDrawable.setState(argState);
            //animateChange(argState);
        } finally {
            mLock.unlock();
        }
    }

    public void animateChange(@PlayPauseDrawable.IconState int argState) {
        mLock.lock();
        try {
            if (mAnimatorSet != null) {
                mAnimatorSet.cancel();
            }

            mAnimatorSet = new AnimatorSet();
            final boolean isPlay = mDrawable.isPlaying();
            final ObjectAnimator colorAnim = ObjectAnimator.ofInt(this, COLOR, isPlay ? mPauseBackgroundColor : mPlayBackgroundColor);
            colorAnim.setEvaluator(new ArgbEvaluator());
            final Animator pausePlayAnim = argState == PlayPauseDrawable.IS_PLAYING ? mDrawable.getPauseAnimator(): mDrawable.getPlayAnimator();
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

    private void setViewSize(final int w, final int h) {
        mDrawable.setBounds(0, 0, w, h);
        mWidth = w;
        mHeight = h;
    }
}
