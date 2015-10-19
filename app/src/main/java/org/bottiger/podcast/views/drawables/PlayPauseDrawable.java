package org.bottiger.podcast.views.drawables;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.util.Log;
import android.util.Property;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 17-10-2015.
 */
public class PlayPauseDrawable extends Drawable {

    @IntDef({IS_PLAYING, IS_PAUSED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IconState {}

    public static final int IS_PLAYING = 1;
    public static final int IS_PAUSED = 2;

    private static final Property<PlayPauseDrawable, Float> PROGRESS =
            new Property<PlayPauseDrawable, Float>(Float.class, "progress") {
                @Override
                public Float get(PlayPauseDrawable d) {
                    return d.getProgress();
                }

                @Override
                public void set(PlayPauseDrawable d, Float value) {
                    d.setProgress(value);
                }
            };

    private final Path mLeftPauseBar = new Path();
    private final Path mRightPauseBar = new Path();
    private final Paint mPaint = new Paint();
    private final RectF mBounds = new RectF();
    private float mPauseBarWidth = -1;
    private float mPauseBarHeight = -1;
    private float mPauseBarDistance = -1;

    private float mWidth = -1;
    private float mHeight = -1;

    private float mProgress = 1;
    private boolean mIsPlaying;

    public PlayPauseDrawable(Context context) {
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mBounds.set(bounds);
        mWidth = mBounds.width();
        mHeight = mBounds.height();
    }

    @Override
    public void draw(Canvas canvas) {

        if (mWidth <= 0) {
            mWidth = canvas.getWidth();
            mHeight = canvas.getHeight();
        }

        if (mPauseBarHeight < 0) {
            mPauseBarHeight = (int)mHeight/3;
            mPauseBarWidth = (int)mWidth/10;
            mPauseBarDistance = (int)mWidth/10;
        }

        mLeftPauseBar.rewind();
        mRightPauseBar.rewind();

        // The current distance between the two pause bars.
        final float barDist = lerp(mPauseBarDistance, 0, mProgress);
        // The current width of each pause bar.
        final float barWidth = lerp(mPauseBarWidth, mPauseBarHeight / 2f, mProgress);
        // The current position of the left pause bar's top left coordinate.
        final float firstBarTopLeft = lerp(0, barWidth, mProgress);
        // The current position of the right pause bar's top right coordinate.
        final float secondBarTopRight = lerp(2 * barWidth + barDist, barWidth + barDist, mProgress);

        // Draw the left pause bar. The left pause bar transforms into the
        // top half of the play button triangle by animating the position of the
        // rectangle's top left coordinate and expanding its bottom width.
        mLeftPauseBar.moveTo(0, 0);
        mLeftPauseBar.lineTo(firstBarTopLeft, -mPauseBarHeight);
        mLeftPauseBar.lineTo(barWidth, -mPauseBarHeight);
        mLeftPauseBar.lineTo(barWidth, 0);
        mLeftPauseBar.close();

        // Draw the right pause bar. The right pause bar transforms into the
        // bottom half of the play button triangle by animating the position of the
        // rectangle's top right coordinate and expanding its bottom width.
        mRightPauseBar.moveTo(barWidth + barDist, 0);
        mRightPauseBar.lineTo(barWidth + barDist, -mPauseBarHeight);
        mRightPauseBar.lineTo(secondBarTopRight, -mPauseBarHeight);
        mRightPauseBar.lineTo(2 * barWidth + barDist, 0);
        mRightPauseBar.close();

        canvas.save();

        // Translate the play button a tiny bit to the right so it looks more centered.
        canvas.translate(lerp(0, mPauseBarHeight / 8f, mProgress), 0);

        // (1) Pause --> Play: rotate 0 to 90 degrees clockwise.
        // (2) Play --> Pause: rotate 90 to 180 degrees clockwise.
        final float rotationProgress = mIsPlaying ? 1 - mProgress : mProgress;
        final float startingRotation = mIsPlaying ? 90 : 0;
        canvas.rotate(lerp(startingRotation, startingRotation + 90, rotationProgress), mWidth / 2f, mHeight / 2f);

        // Position the pause/play button in the center of the drawable's bounds.
        canvas.translate(mWidth / 2f - ((2 * barWidth + barDist) / 2f), mHeight / 2f + (mPauseBarHeight / 2f));

        // Draw the two bars that form the animated pause/play button.
        canvas.drawPath(mLeftPauseBar, mPaint);
        canvas.drawPath(mRightPauseBar, mPaint);

        Log.d("ttt", "mIsPlaying:" + mIsPlaying + " mProgress:" + mProgress);

        canvas.restore();
    }

    public Animator getPausePlayAnimator() {
        final Animator anim = ObjectAnimator.ofFloat(this, PROGRESS, mIsPlaying ? 1 : 0, mIsPlaying ? 0 : 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsPlaying = !mIsPlaying;
            }
        });
        return anim;
    }
    public Animator getPauseAnimator() {
        final Animator anim = ObjectAnimator.ofFloat(this, PROGRESS, 1, 0);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsPlaying = false;
            }
        });
        return anim;
    }


    public Animator getPlayAnimator() {
        final Animator anim = ObjectAnimator.ofFloat(this, PROGRESS, 0, 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsPlaying = true;
            }
        });
        return anim;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void setState(@IconState int argState) {
        if (argState == IS_PLAYING) {
            Log.d("ttt", "is_playing");
            mIsPlaying = true;
            setProgress(0);
        } else {
            Log.d("ttt", "is_paused");
            mIsPlaying = false;
            setProgress(1);
        }
    }

    public @IconState int getIconState() {
        return mIsPlaying ? IS_PLAYING : IS_PAUSED;
    }

    private void setProgress(float progress) {
        mProgress = progress;
        invalidateSelf();
    }

    private float getProgress() {
        return mProgress;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public void setTint(int tintColor) {
        mPaint.setColor(tintColor);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
