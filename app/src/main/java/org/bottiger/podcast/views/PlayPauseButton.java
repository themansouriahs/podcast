package org.bottiger.podcast.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.PlayerStatusProgressData;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.player.exoplayer.ExoPlayerEventListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.StorageUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.dialogs.DialogOpenVideoExternally;
import org.bottiger.podcast.views.drawables.PlayPauseDrawable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_BUFFERING;
import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_IDLE;
import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_READY;

/**
 * TODO: document your custom view class.
 */
public class PlayPauseButton extends View implements DownloadObserver,
                                                                View.OnClickListener,
                                                                Player.EventListener {

    private static final String TAG = PlayPauseButton.class.getSimpleName();

    private static final Property<PlayPauseButton, Integer> COLOR =
            new Property<PlayPauseButton, Integer>(Integer.class, "color") {
                @Override
                public Integer get(PlayPauseButton v) {
                    return v.getColor();
                }

                @Override
                public void set(PlayPauseButton v, Integer value) {
                    v.setColor(value);
                }
            };

    private static final long PLAY_PAUSE_ANIMATION_DURATION = 200;

    private ReentrantLock mLock = new ReentrantLock();

    private PlayPauseDrawable mDrawable;
    private final Paint mPaint = new Paint();
    private final Paint mPaintBackground = new Paint();
    private int mPauseBackgroundColor;
    private int mPlayBackgroundColor;

    private AnimatorSet mAnimatorSet;
    private int mBackgroundColor;

    private @ColorRes
    int color1 = R.color.colorPrimaryDark;
    private @ColorRes int color2 = R.color.colorPrimaryDark;

    private static final boolean DEBUG = false;

    private static final boolean DRAW_PROGRESS          = true;
    private static final boolean DRAW_PROGRESS_MARKER   = true;

    private static final int DRAW_ANGLE_OFFSET = -90;
    private static final int FPS = 60;
    private static final long ANIMATION_DURATION = 10000; // in ms, 10 seconds
    private static final long ANIMATION_APPEAR_THRESHOLD = 250; // time before we show the animation
    private static final long ANIMATION_HIDE_THRESHOLD = 1000; // minimum time we display the animation if it appears

    private long mStartTime = System.currentTimeMillis();
    private long mPreparingAnimationStarted = -1;
    private boolean mAnimationNeedsAligning = false;

    private static final AccelerateDecelerateInterpolator interperter1 = new AccelerateDecelerateInterpolator();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PLAYLIST, FEEDVIEW, DISCOVERY_FEEDVIEW, OTHER})
    public @interface ButtonLocation {}
    public static final int PLAYLIST = 1;
    public static final int FEEDVIEW = 2;
    public static final int DISCOVERY_FEEDVIEW = 3;
    public static final int OTHER = 4;

    private @ButtonLocation int mLocation = OTHER;

    private static final int START_ANGLE = -90;
    private static final int DRAW_OFFSET = 0;
    private static final int DRAW_WIDTH = 6;

    private double mLastProgressStart = -1;
    private double mLastProgressEnd = -1;

    private @SoundWavesPlayerBase.PlayerState int mStatus = STATE_IDLE;

    private IEpisode mEpisode;

    protected Context mContext;

    private RectF bounds = new RectF();
    private Rect boundsRound = new Rect();

    boolean mFoundStart = false;
    boolean mFoundEnd = false;

    private Animation rotateAnimation;

    private boolean mDrawBackground;
    private int mProgressPercent = 0;

    protected Paint paint;
    private Paint paintBorder;

    private int mPaintColor;
    private int mPaintBorderColor = Color.WHITE;

    private Disposable mRxDisposable;

    public PlayPauseButton(Context context) {
        super(context, null);
        init(context);
    }

    public PlayPauseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        initAttr(attrs);
    }

    public PlayPauseButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        init(context);
        initAttr(attrs);
    }

    private void init(Context argContext) {

        setWillNotDraw(false);
        mBackgroundColor = ContextCompat.getColor(argContext, color1);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);

        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setStyle(Paint.Style.FILL);

        mDrawable = new PlayPauseDrawable(argContext);
        mDrawable.setCallback(this);

        mPauseBackgroundColor = ContextCompat.getColor(argContext, color1);
        mPlayBackgroundColor = ContextCompat.getColor(argContext, color2);

        mContext = argContext;

        mPaintColor = ContextCompat.getColor(mContext, R.color.colorPrimaryDark); // colorPrimaryDark

        paint = new Paint(Paint.LINEAR_TEXT_FLAG);
        paint.setColor(mPaintColor);
        paint.setTextSize(12.0F);
        paint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        paint.setStrokeWidth(1F);

        paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setColor(mPaintBorderColor);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(DRAW_WIDTH);

        if (Build.VERSION.SDK_INT >= 16) {
            //setBackground(null);
        }

        if (isInEditMode()) {
            return;
        }

        setOnClickListener(this);

        mRxDisposable = SoundWaves.getRxBus2()
                .toFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .ofType(PlayerStatusProgressData.class)
                .filter(new Predicate<PlayerStatusProgressData>() {
                    @Override
                    public boolean test(PlayerStatusProgressData playerStatusProgressData) throws Exception {
                        return playerStatusProgressData.getEpisode().equals(getEpisode());
                    }
                })
                .subscribe(new Consumer<PlayerStatusProgressData>() {
                    public void accept(PlayerStatusProgressData playerStatusProgressData){
                        setProgressMs(playerStatusProgressData);
                    }
                });
    }

    private void initAttr(AttributeSet attrs) {
        TypedArray a = mContext.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PlayPauseButton,
                0, 0);

        try {
            mDrawBackground = a.getBoolean(R.styleable.PlayPauseButton_drawBackground, true);
        } finally {
            a.recycle();
        }

    }

    private int getColor() {
        return mBackgroundColor;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }

    @MainThread
    public void setColor(int color) {
        mBackgroundColor = color;
        mPlayBackgroundColor = color;
        mPauseBackgroundColor = color;
        invalidate();
    }

    /*
    This happens when the playlist is empty and an episode is about to be played.
    The state change fires prior to the binding.
     */
    @Nullable
    @Override
    public IEpisode getEpisode() {
        return mEpisode;
    }

    public synchronized void setEpisode(IEpisode argEpisode, @ButtonLocation int argLocation) {
        this.mLocation = argLocation;
        this.mEpisode = argEpisode;

        setProgressMs(new PlayerStatusProgressData(mEpisode));
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisode = null;
        mRxDisposable.dispose();
    }

    @MainThread
    public void setStatus(@SoundWavesPlayerBase.PlayerState int argPlayerStatus) {

        IEpisode episode = getEpisode();

        if (episode == null) {
            return;
        }

        if (episode.isPlaying()) {
            if (IsDisplayingPlayIcon()) {
                animateChangeFrom(PlayPauseDrawable.IS_PAUSED);
            }
        } else {
            if (!IsDisplayingPlayIcon()) { // we are not displaying the play icon, we should
                animateChangeFrom(PlayPauseDrawable.IS_PLAYING);
            }
        }

        mStatus = argPlayerStatus;

        if (mStatus == STATE_BUFFERING) {
            mStartTime = System.currentTimeMillis();
        }

        this.invalidate();
    }

    public void setColor(@ColorInt int argColor, @ColorInt int argOuterColor) {
        final float scale_base = 1.3f;

        float scale = UIUtils.isInNightMode(getResources()) ? 1/scale_base : scale_base;
        int maxColor = 255;

        float red = Math.min(Color.red(argColor)*scale, maxColor);
        float green = Math.min(Color.green(argColor)*scale, maxColor);
        float blue = Math.min(Color.blue(argColor)*scale, maxColor);

        int newColor = Color.argb(maxColor, (int)red, (int)green, (int)blue);

        setColor(newColor);
        paintBorder.setColor(argOuterColor);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setColor(mBackgroundColor);
        final float radius_circle = Math.min(getWidth(), getHeight()) / 2f;
        final float cx = getWidth()/2f;
        final float cy = getHeight()/2f;
        canvas.drawCircle(cx, cy, radius_circle, mPaint);

        //float transY = (getHeight()-getWidth())/2f;
        //canvas.translate(0, transY);
        mDrawable.draw(canvas);

        if (!DRAW_PROGRESS) {
            return;
        }

        int contentWidth = getWidth();
        int contentHeight = getHeight();

        int minSize = Math.min(contentWidth, contentHeight);

        int centerX = contentWidth/2;
        int centerY = contentHeight/2;

        float radius = centerX-DRAW_WIDTH;

        float drawOffset =  DRAW_WIDTH/2;
        boolean updateOutline = bounds == null;

        bounds.left = contentWidth > minSize ? (contentWidth-minSize)/2f + drawOffset : drawOffset;
        bounds.top = (contentHeight-minSize)/2f + drawOffset;
        bounds.right = minSize - drawOffset; //contentWidth - drawOffset;
        bounds.bottom = (contentHeight-minSize)/2f + minSize - drawOffset; //contentWidth - drawOffset;

        if (DEBUG) {
            Log.d(TAG, "onDraw. Preparing => " + (mStatus == STATE_BUFFERING) + " status: " + mStatus);
        }

        double elapsedTime = System.currentTimeMillis() - mStartTime;
        boolean showRotatingAnimation = mStatus == STATE_BUFFERING;// || animationStartedLessThanOneSecondAgo(mPreparingAnimationStarted);
        boolean refreshButton = false;

        double defaultStartAngle = 0.0;
        double defaultEndAngle = 360.0;

        if (showRotatingAnimation) {
            // Draw undetermint progress indicator
            refreshButton = true;
            mFoundStart = false;
            mFoundEnd = false;

            mAnimationNeedsAligning = true;
            if (mPreparingAnimationStarted == -1) {
                mPreparingAnimationStarted = System.currentTimeMillis();
            }

            double angles[] = getCircleAngles(elapsedTime);

            defaultStartAngle = angles[0];
            defaultEndAngle = angles[1];

        } else if (!mAnimationNeedsAligning) {
            // Draw normal progress
            mPreparingAnimationStarted = -1;
            defaultEndAngle = getProgressAngle(mProgressPercent);
        } else {
            // Align the rotating progress indication with the current progress
            refreshButton = true;

            double angles[] = getCircleAngles(elapsedTime);

            double currentStart = angles[0];
            double currentEnd = angles[1];

            double endGoal = getProgressAngle(mProgressPercent);
            double startGoal = getProgressAngle(0);

            if (Math.abs(currentStart-startGoal) > Math.abs(mLastProgressStart) || mFoundStart) {
                currentStart = 0;
                mFoundStart = true;
            }

            if (Math.abs(currentEnd-endGoal) > Math.abs(mLastProgressEnd) || mFoundEnd) {
                currentEnd = endGoal;
                mFoundEnd = true;
            }

            mLastProgressStart = currentStart;
            mLastProgressEnd = currentEnd;

            defaultStartAngle = currentStart;
            defaultEndAngle = currentEnd;

            if (mFoundStart && mFoundEnd) {
                refreshButton = false;
                mAnimationNeedsAligning = false;
            }

        }

        defaultStartAngle = defaultStartAngle + DRAW_ANGLE_OFFSET;
        defaultEndAngle = defaultEndAngle + DRAW_ANGLE_OFFSET;

        if (DEBUG) {
            Log.v(TAG, "startAngle: " + defaultStartAngle + " endAngle: " + defaultEndAngle + " elapsed:" + elapsedTime + " lastStart: " + mLastProgressStart + " lastEnd: " + mLastProgressEnd);
        }

        int strokeWidth = contentWidth/15; // DRAW_WIDTH;
        paintBorder.setStrokeWidth(strokeWidth);
        canvas.drawArc(bounds, (float)defaultStartAngle, (float)(defaultEndAngle - defaultStartAngle), false, paintBorder);

        if (refreshButton) {
            this.postInvalidateDelayed(1000 / FPS);
        }
    }

    private static double[] getCircleAngles(double argElapsedTime) {
        double angle = (argElapsedTime / 10.0) % 360;

        double zeroToOne = (angle) / 360.0f;
        double zeroToTwo = zeroToOne * 2;
        double minusOneToOne = zeroToTwo - 1;
        double onezeroone = Math.abs(Math.abs(minusOneToOne) * -1);

        double[] out = new double[2];

        out[0] = angle;
        out[1] = interperter1.getInterpolation((float)onezeroone) * 360;

        return out;
    }
    
    private void setProgressMs(PlayerStatusProgressData argPlayerProgress) {

        // copy from seekbar

        long progressMs = argPlayerProgress.getProgress();

        if (progressMs < 0) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("Progress must be positive");
            }
            return;
        }
        float progress = 0;

        IEpisode episode = getEpisode();

        if (episode == null)
            return;

        float duration = episode.getDuration();

        if (duration <= 0) {
            Log.d("Warning", "Seekbar state may be invalid");
            return;
        }

        try {
            progress = progressMs / duration * 100;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgressPercent((int) progress);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, @SoundWavesPlayerBase.PlayerState int playbackState) {
        onPlayerStateChange(playbackState);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    private void onPlayerStateChange(@SoundWavesPlayerBase.PlayerState int argPlayerStatus) {

        IEpisode episode = getEpisode();

        if (episode == null)
            return;

        Playlist playlist = SoundWaves.getAppContext(getContext()).getPlaylist();
        if (!episode.equals(playlist.first())) {

            setStatus(STATE_READY);
            return;
        }

        setStatus(argPlayerStatus);
    }

    @Override
    public void setProgressPercent(int argProgress) {
        mProgressPercent = argProgress;
        invalidate();
    }

    @MainThread
    @Override
    public void onClick(View view) {

        PlayerService ps = PlayerService.getInstance();

        if (ps == null)
            return;

        if (mEpisode.getUrl() == null)
            return;

        boolean isPlaying = PlayerService.isPlaying() && mEpisode.equals(ps.getCurrentItem());

        if (isPlaying) {
            animateChangeFrom(PlayPauseDrawable.IS_PLAYING);
        } else {
            animateChangeFrom(PlayPauseDrawable.IS_PAUSED);
            setStatus(STATE_BUFFERING);
        }

        // If the file is a video we offer to open it in another external player
        if (mEpisode.isVideo()) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String hasAskedKey = getResources().getString(R.string.pref_ask_about_video_key);
            boolean hasAsked = prefs.getBoolean(hasAskedKey, false);

            boolean doOpenExternally = false;

            if (!hasAsked) {
                DialogOpenVideoExternally dialogOpenVideoExternally = DialogOpenVideoExternally.newInstance(mEpisode);

                try {
                    Activity activity = (Activity) mContext;
                    dialogOpenVideoExternally.show(activity.getFragmentManager(), "dialog");
                } catch (ClassCastException cce) {
                    Log.wtf(TAG, "Could not case the context to an activity. " + cce.toString());
                } finally {
                    prefs.edit().putBoolean(hasAskedKey, true).apply();
                }

                /**
                 * The Dialog is show async, and since we are in a view and not an activity or a fragment
                 * There is no clean way to get a callback. Therefore we are going to handle everything inside the
                 * Dialog
                 */
                return;
            }

            boolean openExternallyDefault = getResources().getBoolean(R.bool.pref_open_video_externally_default);
            String openExternallyKey = getResources().getString(R.string.pref_open_video_externally_key);
            doOpenExternally = prefs.getBoolean(openExternallyKey, openExternallyDefault);


            if (doOpenExternally) {
                openVideoExternally(mEpisode, mContext);
                return;
            }

        }

        ps.toggle(mEpisode);

        IAnalytics.EVENT_TYPE type = getEventType();
        if (type != null) {
            if (SoundWaves.sAnalytics != null)
                SoundWaves.sAnalytics.trackEvent(type);
        }
    }

    public static void openVideoExternally(@NonNull IEpisode argEpisode, @NonNull Context argContext) {
        Uri uri = argEpisode.getFileLocation(IEpisode.PREFER_LOCAL, argContext);

        String mimetype;
        if (argEpisode.isDownloaded(argContext)) {
            mimetype = StorageUtils.getMimeType(argEpisode.getFileLocation(IEpisode.REQUIRE_LOCAL, argContext).toString());
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(argEpisode.getFileLocation(IEpisode.REQUIRE_REMOTE, argContext).toString());
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, mimetype);
        //argContext.startActivity(Intent.createChooser(intent, argContext.getString(R.string.choose_player_for_open_video_externally)));
        argContext.startActivity(intent);
    }

    public void setColor(ColorExtractor value) {
        @ColorInt int innerColor = value.getPrimary() != -1 ? value.getPrimary() : ContextCompat.getColor(mContext, R.color.colorPrimary);
        @ColorInt int outerColor = value.getPrimaryTint() != -1 ? value.getPrimaryTint() : ContextCompat.getColor(mContext, R.color.colorBgPrimary);

        setColor(innerColor, outerColor);
    }

    private float getProgressAngle(int argProgress) {
        return argProgress*3.6F;
    }

    @Nullable
    private IAnalytics.EVENT_TYPE getEventType() {
        if (mLocation == PLAYLIST) {
            return IAnalytics.EVENT_TYPE.PLAY_FROM_PLAYLIST;
        }

        if (mLocation == FEEDVIEW) {
            return IAnalytics.EVENT_TYPE.PLAY_FROM_FEEDVIEW;
        }

        if (mLocation == DISCOVERY_FEEDVIEW) {
            return IAnalytics.EVENT_TYPE.PLAY_FROM_FEEDVIEW;
        }

        return null;
    }


    //
    // http://stackoverflow.com/questions/27497987/android-elevation-is-not-showing-a-shadow-under-a-customview
    //

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if  (isInEditMode()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CustomOutline(bounds));
        }
    }

    @TargetApi(21)
    private class CustomOutline extends ViewOutlineProvider {

        RectF bounds = null;

        CustomOutline(RectF argRectF) {
            bounds = argRectF;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            if (bounds != null) {
                bounds.round(boundsRound);
                outline.setOval(boundsRound);
            }
        }
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
