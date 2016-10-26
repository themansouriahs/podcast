package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;

import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_READY;

/**
 * Created by apl on 03-09-2014.
 */
public class PlayerSeekbar extends SeekBar implements PaletteListener, ExoPlayer.EventListener {

    private static final String TAG = "PlayerSeekbar";
    private static final int RANGE_MAX = 1000;

    private boolean mPaintSeekInfo = false;

    private View mOverlay = null;
    private IEpisode mEpisode = null;

    private TextView mBackwards;
    private TextView mCurrent;
    private TextView mForwards;

    private String mBackwardsText = "";
    private String mCurrentText = "";
    private String mForwardText = "";

    private int mStartSeekPosition = -1;
    private long mDurationMs = -1;

    private boolean mIsPlaying = false;
    private boolean mIsTouching = false;

    private CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(0 ,0);
    private int[] loc = new int[2];

    // For the Chronometer
    private boolean mVisible;
    private boolean mStarted;
    private boolean mRunning;

    private static final int TICK_WHAT = 2;

    private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

        private final int cDrawThresshold = 10;
        private int mThressholdCounter = -1;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d("PlayerSeekbar state", "onStopTrackingTouch");
            mIsTouching = false;
            mPaintSeekInfo = false;
            Log.d("mPaintSeekInfo", "onStopTracking mPaintSeekInfo => " + mPaintSeekInfo);
            mThressholdCounter = -1;

            if (!validateState()) {
                return;
            }

            long timeMs = mEpisode.getDuration() * seekBar.getProgress()
                    / RANGE_MAX;

            if (mEpisode.equals(PlayerService.getCurrentItem())) {
                PlayerService.getInstance().seek(timeMs);
            } else {
                mEpisode.setOffset(getContext().getContentResolver(), timeMs);
                setProgressMs(timeMs);
            }

            invalidate();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d("PlayerSeekbar state", "onStartTrackingTouch");
            if (!validateState()) {
                return;
            }

            if (mDurationMs < 0) {
                mDurationMs = mEpisode.getDuration();
            }

            Log.d("mPaintSeekInfo", "onStartTracking mPaintSeekInfo => " + mPaintSeekInfo);
            mIsTouching = true;
            mThressholdCounter = cDrawThresshold;
            mStartSeekPosition = seekBar.getProgress();

            long timeMs = mEpisode.getDuration() * seekBar.getProgress()
                    / RANGE_MAX;
            mEpisode.setOffset(null, timeMs);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
            Log.d("PlayerSeekbar state", "Seekbar onProgressCanged");
            if (!fromUser) {
                Log.d("PlayerSeekbar", "Seekbar change , but not by the user");
                return;
            }

            Log.d("mPaintSeekInfo", "ProgressTracking1 mPaintSeekInfo => " + mPaintSeekInfo + " mThressholdCounter => " + mThressholdCounter);
            if (mThressholdCounter > 0) {
                mThressholdCounter--;
            } else {
                mPaintSeekInfo = true;
            }
            Log.d("mPaintSeekInfo", "ProgressTracking2 mPaintSeekInfo => " + mPaintSeekInfo + " mThressholdCounter => " + mThressholdCounter);

            if (mDurationMs < 1) {
                Log.d("PlayerSeekbar", "Duration unknown. Skipping overlay");
                mPaintSeekInfo = false;

            }

            float currentSeekPosition = (float)progress / RANGE_MAX;
            float SeekDiff = (float)(progress-mStartSeekPosition) / RANGE_MAX;

            long currentPositionMs = (long)(currentSeekPosition*mDurationMs);
            long diffMs = (long)(SeekDiff*mDurationMs);

            if (SeekDiff > 0) {
                mForwardText = "+" + StrUtils.formatTime(diffMs);
                mBackwardsText = "";
            } else {
                mForwardText = "";
                mBackwardsText = "-" + StrUtils.formatTime(-diffMs);
            }

            mCurrentText = StrUtils.formatTime(currentPositionMs);
        }
    };


    public PlayerSeekbar(Context context) {
        super(context);
        init();
    }

    public PlayerSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayerSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public PlayerSeekbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        setMax(RANGE_MAX);
        setOnSeekBarChangeListener(onSeekBarChangeListener);
        mIsPlaying = PlayerService.isPlaying();
    }

    public void setEpisode(@NonNull IEpisode argEpisode) {
        mEpisode = argEpisode;

        int progress = 0;

        if (mEpisode.getOffset() > 0 && mEpisode.getDuration() > 0) {
            float progressf = (float)mEpisode.getOffset() / mEpisode.getDuration();
            progress = (int)(progressf*RANGE_MAX);
        }

        setProgress(progress);
    }

    public void setOverlay(View argLayout) {
        mOverlay = argLayout;
        mBackwards = (TextView) mOverlay.findViewById(R.id.seekbar_time_backwards);
        mCurrent =   (TextView) mOverlay.findViewById(R.id.seekbar_time_current);
        mForwards =  (TextView) mOverlay.findViewById(R.id.seekbar_time_forward);
    }

    @Nullable
    public View getViewOverlay() {
        return mOverlay;
    }

    @Nullable
    public IEpisode getEpisode() {
        if (!validateState()) {
            return null;
        }
        return mEpisode;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.v(TAG, "draw with mPaintSeekInfo => " + mPaintSeekInfo);

        if (isInEditMode()) {
            return;
        }

        if (mPaintSeekInfo) {
            Log.v(TAG, "Draw seekinfo");
            if (params.height == 0) {
                mOverlay.setVisibility(INVISIBLE);
                params.height = mOverlay.getHeight();
                params.width = mOverlay.getWidth();
                mOverlay.bringToFront();
            }

            mBackwards.setText(mBackwardsText);
            mCurrent.setText(mCurrentText);
            mForwards.setText(mForwardText);

            this.getLocationInWindow(loc);

            int offset =  (int)UIUtils.convertDpToPixel(50, getContext()); //FIXME this.getHeight()*4;
            int translationY = (int)((View)this.getParent()).getTranslationY();
            Log.v(TAG, "trans => " + translationY);
            Log.v(TAG, "loc0 => " + loc[0] + " loc0 => " + loc[1] + " offset => " + offset);
            params.setMargins(0, offset, 0, 0);

            if (mOverlay != null) {
                mOverlay.getLocationOnScreen(loc);
                Log.v(TAG, "Overlay: loc0 => " + loc[0] + " loc0 => " + loc[1]);
                mOverlay.setLayoutParams(params);
                mOverlay.setVisibility(VISIBLE);
            }
        } else {
            Log.d("PlayerSeekbar", "Remove seekinfo");
            if (mOverlay != null) {
                mOverlay.setVisibility(GONE);
            }
        }
    }

    private void setProgressMs(long progressMs) {
        if (isTouching()) {
            return;
        }

        if (progressMs < 0) {
            Throwable ise = new IllegalStateException("Progress must be positive");
            Log.e(TAG, "Progress must be positive ( " + progressMs + " )", ise);
            return;
        }

        if (mEpisode == null)
            return;

        float progress = 0;
        float duration = mEpisode.getDuration();

        if (duration <= 0) {
            Log.d("Warning", "Seekbar state may be invalid");
            return;
        }

        try {
            progress = progressMs / duration * RANGE_MAX;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgress((int) progress);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(TAG, event.toString());
        Log.v(TAG, "------------");
        requestParentTouchRecursive(getParent(), true);

        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                onSeekBarChangeListener.onStopTrackingTouch(this);
                invalidate();
                requestParentTouchRecursive(getParent(), false);
                break;
            case MotionEvent.ACTION_DOWN:
                onSeekBarChangeListener.onStartTrackingTouch(this);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                int pos = (int) (getMax() * event.getX() / getWidth());
                Log.v(TAG, "pos => " + pos);
                setProgress(pos);
                onSeekBarChangeListener.onProgressChanged(this, pos, true);
                invalidate();
                break;

            default:
            case MotionEvent.ACTION_CANCEL:
                onSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                requestParentTouchRecursive(getParent(), false);
                invalidate();
                break;
        }
        return true;

    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        Palette.Swatch swatch = argChangedPalette.getVibrantSwatch();
        if (swatch==null)
            return;

        int color = swatch.getRgb();
        ColorDrawable dc = new ColorDrawable(color);
        dc.setAlpha(100);
        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return mEpisode.getArtwork(getContext());
    }

    private boolean validateState() {
        if (mEpisode == null) {
            if (BuildConfig.DEBUG) {
                return false; //throw new IllegalStateException("Episode needs to be set");
            } else {
                return false;
            }
        }

        if (mOverlay == null) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("Overlay needs to be set");
            } else {
                return false;
            }
        }

        return true;
    }

    private void requestParentTouchRecursive(@NonNull ViewParent argThisParent, boolean argDisallowTouch) {
        argThisParent.requestDisallowInterceptTouchEvent(argDisallowTouch);

        ViewParent nextParent = argThisParent.getParent();

        if (nextParent != null) {
            //Log.d("PlayerSeekbar", nextParent.toString() + " -> " + argDisallowTouch);
            requestParentTouchRecursive(nextParent, argDisallowTouch);
        }
    }

    private boolean isTouching() {
        return mIsTouching;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, @SoundWavesPlayerBase.PlayerState int playbackState) {
        if (!validateState()) {
            return;
        }

        mIsPlaying = playWhenReady && playbackState == STATE_READY;

        long playbackPosition = SoundWaves.getAppContext(getContext()).getPlayer().getCurrentPosition();

        float currentPositionMs = Math.max(playbackPosition, 0);
        double episodeLenghtMS = (double)mEpisode.getDuration();

        if (episodeLenghtMS < 0 || currentPositionMs > episodeLenghtMS) {
            return;
        }

        if (mIsPlaying) {
            float progress = currentPositionMs / mEpisode.getDuration() * RANGE_MAX;
            setProgress((int) progress);
        }
    }


    private void updateRunning() {
        boolean running = mVisible && mStarted;
        if (running != mIsPlaying) {
            if (running) {
                setProgressMs(SoundWaves.getAppContext(getContext()).getPlayer().getCurrentPosition());
                mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), 1000);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
            mIsPlaying = running;
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            if (mIsPlaying) {
                //updateText(SystemClock.elapsedRealtime());
                setProgressMs(SoundWaves.getAppContext(getContext()).getPlayer().getCurrentPosition());
                sendMessageDelayed(Message.obtain(this, TICK_WHAT), 1000);
            }
        }
    };

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }
}