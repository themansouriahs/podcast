package org.bottiger.podcast.views;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.transition.ChangeBounds;
import android.support.transition.Scene;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ivbaranov.mfb.MaterialFavoriteButton;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.bottiger.podcast.PlaylistFragment;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.PlayerChapterAdapter;
import org.bottiger.podcast.flavors.Activities.Constants;
import org.bottiger.podcast.flavors.Activities.VendorActivityTracker;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.NewPlayerEvent;
import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PlayerHelper;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.rxbus.RxBusSimpleEvents;
import org.bottiger.podcast.views.dialogs.DialogChapters;
import org.bottiger.podcast.views.dialogs.DialogPlaybackSpeed;

import java.util.Calendar;

import io.reactivex.functions.Consumer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.bottiger.podcast.flavors.Activities.Constants.OTHER;
import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_READY;

/**
 * Created by apl on 30-09-2014.
 */
public class TopPlayer extends RelativeLayout implements ScrollingView, NestedScrollingChild {

    private static final String TAG = TopPlayer.class.getSimpleName();

    private static final long TIMER_NOT_SET = -1;
    private static final int DEFAULT_SLEEP_TIME_MIN = 30;

    public NestedScrollingChildHelper scrollingChildHelper;
    private ViewConfiguration mViewConfiguration;

    private Context mContext;

    public static int sizeSmall                 =   -1;
    public static int sizeMedium                =   -1;
    public static int sizeLarge                 =   -1;
    public static int sizeActionbar             =   -1;

    public static int sizeStartShrink           =   -1;
    public static int sizeShrinkBuffer           =   -1;

    private static int sScreenHeight = -1;

    private int mControlHeight = -1;

    private Scene mPrimaryScene;
    private Scene mDrivingScene;
    private @Constants.Activities int mCurrentActivity = OTHER;

    private float screenHeight;

    private String mDoFullscreentKey;
    private boolean mFullscreen = false;

    private String mDoDisplayTextKey;
    private boolean mDoDisplayText = false;

    @ColorInt private int mTextColor = Color.BLACK;

    @Nullable IEpisode mCurrentEpisode;

    private TopPlayer mMainLayout;
    private ViewGroup mControls;
    @Nullable private View mDrivingLayout = null;

    @Nullable private TextView mEpisodeTitle;
    @Nullable private TextView mEpisodeInfo;
    @Nullable private TextViewObserver mCurrentTime;
    @Nullable private TextView mTotalTime;
    @Nullable private PlayPauseImageView mPlayPauseButton;
    @Nullable private PlayerSeekbar mPlayerSeekbar;
    @Nullable private DownloadButtonView mPlayerDownloadButton;
    @Nullable private MaterialFavoriteButton mFavoriteButton;
    @Nullable private ImageView mPlaylistUpArrow;
    @Nullable private View mTriangle;
    @Nullable private ImageView mPhoto;

    @Nullable private ViewStub mMoreButtonsStub;
    @Nullable private LinearLayout mExpandedActionsBar;
    @Nullable private PlayerButtonView mFullscreenButton;
    @Nullable private Button mSleepButton;
    @Nullable private ImageView mChapterButton;
    @Nullable private Button mSpeedButton;

    @Nullable private TextView mDrivingTitle;
    @Nullable private PlayerSeekbar mDrivingSeekbar;
    @Nullable private TextViewObserver mDrivingCurrentTime;
    @Nullable private TextView mDrivingTotalTime;
    @Nullable private PlayPauseImageView mDrivingPlayPause;
    @Nullable private ImageView mDrivingFastForward;
    @Nullable private ImageView mDrivingReverse;
    @Nullable private ImageView mDrivingPhoto;

    @NonNull private GenericMediaPlayerInterface mPlayer;

    @Nullable private ImageView mFastForwardButton;
    @Nullable private ImageView mRewindButton;
    @Nullable private ImageButton mMoreButton;

    @Nullable private Overlay mOverlay;

    private Subscription mRxPlayerChanged;
    private Subscription mRxTopEpisodeChanged;

    @Nullable private CountDownTimer mCountDownTimer;
    private long mCountDownTimeLeft = TIMER_NOT_SET;

    private boolean mPlaylistEmpty = true;

    private PlayerLayoutParameter mLargeLayout = new PlayerLayoutParameter();

    private GestureDetectorCompat mGestureDetector;
    private TopPLayerScrollGestureListener mTopPLayerScrollGestureListener;

    private @ColorInt int mBackgroundColor = -1;

    private SharedPreferences prefs;
    public ScrollerCompat mScroller;

    private class PlayerLayoutParameter {
        public int SeekBarLeftMargin;
        public int PlayPauseSize;
    }

    public TopPlayer(Context context) {
        super(context, null);
        scrollingChildHelper = new NestedScrollingChildHelper(this);
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
        Log.v(TAG, "App start time: " + System.currentTimeMillis());

        mTopPLayerScrollGestureListener = new TopPLayerScrollGestureListener();
        scrollingChildHelper.setNestedScrollingEnabled(true);

        mRxPlayerChanged = getPlayerSubscription();
        mGestureDetector = new GestureDetectorCompat(argContext,mTopPLayerScrollGestureListener);
        mPlayer = SoundWaves.getAppContext(getContext()).getPlayer();

        mViewConfiguration = ViewConfiguration.get(argContext);

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
        sScreenHeight = UIUtils.getScreenHeight(argContext);

        sizeSmall = argContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_minimum);
        sizeMedium = argContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_medium);
        //sizeLarge = sScreenHeight- 0;//argContext.getResources().getDimensionPixelSize(R.dimen.top_player_size_maximum_bottom);

        sizeStartShrink = sizeSmall+sizeShrinkBuffer;
        sizeActionbar = argContext.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(false);
        }

        setClipChildren(false);

        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    //viewWidth = view.getWidth();
                    sizeLarge = TopPlayer.this.getHeight();
                }
            });
        }
    }


    @SuppressWarnings("ResourceType")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (sizeLarge > 0 && !mFullscreen){
            int hSize = MeasureSpec.getSize(heightMeasureSpec);
            int hMode = MeasureSpec.getMode(heightMeasureSpec);

            int height = -1;
            int mode = MeasureSpec.EXACTLY;

            switch (hMode){
                case MeasureSpec.AT_MOST:
                    height = Math.min(hSize, sizeLarge);
                    break;
                case MeasureSpec.UNSPECIFIED:
                    height = (int) Math.max(hSize, screenHeight);
                    break;
                case MeasureSpec.EXACTLY: {
                    height = mPlaylistEmpty ? hSize : Math.min(hSize, sizeLarge);
                    mode = MeasureSpec.EXACTLY;
                    break;
                }
            }

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, mode);

            screenHeight = height;
        }


        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();


        mMainLayout = this;
        mControls = (ViewGroup) findViewById(R.id.top_player_controls);

        ViewGroup sceneRoot = mControls;
        ViewGroup sceneLayout = (ViewGroup) findViewById(R.id.scene_layout);

        mPrimaryScene = new Scene(sceneRoot, sceneLayout);
        //mDrivingScene = Scene.getSceneForLayout(sceneRoot, R.layout.playlist_top_player_driving, getContext());

        getPlayerControlHeight(mControls);

        mEpisodeTitle           =    (TextView) findViewById(R.id.player_title);
        mEpisodeInfo            =    (TextView) findViewById(R.id.player_podcast);
        mFavoriteButton         =    (MaterialFavoriteButton) findViewById(R.id.favorite);

        mCurrentTime            =    (TextViewObserver) findViewById(R.id.current_time);
        mTotalTime              =    (TextView) findViewById(R.id.total_time);

        mPlayPauseButton        =    (PlayPauseImageView) findViewById(R.id.playpause);
        mPlayerSeekbar          =    (PlayerSeekbar) findViewById(R.id.top_player_seekbar);
        mPlayerDownloadButton   =    (DownloadButtonView) findViewById(R.id.download);

        mPlayPauseButton = (PlayPauseImageView) findViewById(R.id.playpause);
        mFavoriteButton = (MaterialFavoriteButton) findViewById(R.id.favorite);
        mFastForwardButton = (ImageView) findViewById(R.id.top_player_fastforward);
        mRewindButton = (ImageView) findViewById(R.id.top_player_rewind);
        mPhoto = (ImageView) findViewById(R.id.session_photo);
        mMoreButton = (ImageButton) findViewById(R.id.player_more_button);
        mPlayerSeekbar = (PlayerSeekbar) findViewById(R.id.top_player_seekbar);
        mPlaylistUpArrow = (ImageView) findViewById(R.id.playlist_up_arrow);

        mMoreButtonsStub = (ViewStub) findViewById(R.id.expanded_action_bar);

        mTriangle = findViewById(R.id.visual_triangle);

        int mPlayPauseLargeSize = mPlayPauseButton.getLayoutParams().height;
        mPlayPauseButton.setIconColor(Color.WHITE);

        mLargeLayout.SeekBarLeftMargin = 0;
        mLargeLayout.PlayPauseSize = mPlayPauseLargeSize;

        final Playlist playlist = SoundWaves.getAppContext(mContext).getPlaylist();

        boolean isRecyclerViewEmpty = playlist.size()<2;

        setPlaylistEmpty(isRecyclerViewEmpty);

        setFullscreenImagePadding(getContext());

        if (mDoDisplayText) {
            //showText();
            onClickMoreButton(mMoreButton);
        }

        mMoreButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View argView) {
                onClickMoreButton(argView);
            }
        });


        mFavoriteButton.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
            @Override
            public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                IEpisode episode = playlist.first();
                if (episode instanceof FeedItem) {
                    FeedItem feedItem = (FeedItem)episode;
                    feedItem.setIsFavorite(favorite);
                }
            }
        });


        setPlayer();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        SoundWaves.getRxBus().toObserverable()
                .ofType(RxBusSimpleEvents.PlaybackEngineChanged.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(new Func1<RxBusSimpleEvents.PlaybackEngineChanged, Boolean>() {
                    @Override
                    public Boolean call(RxBusSimpleEvents.PlaybackEngineChanged playbackEngineChanged) {
                        return mSpeedButton != null;
                    }
                })
                .subscribe(new Action1<RxBusSimpleEvents.PlaybackEngineChanged>() {
                    @Override
                    public void call(RxBusSimpleEvents.PlaybackEngineChanged event) {
                        assert  mSpeedButton != null;

                        mSpeedButton.setText(getContext().getString(R.string.speed_multiplier, event.speed));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });

    }

    public void setOverlay(@Nullable Overlay argOverlay) {
        mOverlay = argOverlay;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void sleepButtonPressed() {
        int defaultminutes = mContext.getResources().getInteger(R.integer.player_sleep_default);
        sleepButtonPressed(defaultminutes);
    }

    private void sleepButtonPressed(int argMinutes) {
        String toast = getResources().getQuantityString(R.plurals.player_sleep, argMinutes, argMinutes);
        String toast_cancel = getResources().getString(R.string.player_sleep_cancel);

        long millis = argMinutes * 60 * 1000;

        setSleepTimer(argMinutes);

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer.onFinish();
        }

        if (argMinutes < 0) {
            UIUtils.disPlayBottomSnackBar(this, toast_cancel, null, true);
        } else {
            setCountDownText(millis);
            UIUtils.disPlayBottomSnackBar(this, toast, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    openTimePicker();
                }
            }, R.string.snackbar_change, true);
        }
    }

    private boolean setSleepTimer(int minutes) {
        if (minutes < 0) {
            mPlayer.cancelFadeOut();
            return true;
        }

        int onemin = 1000 * 60;
        mPlayer.FadeOutAndStop(onemin*minutes);
        return true;
    }

    public void bind(@Nullable final IEpisode argEpisode) {

        // FIXME remove
        //if (mCurrentEpisode == argEpisode)
        //    return;

        mCurrentEpisode = argEpisode;

        if (argEpisode == null)
            return;

        if (mRxTopEpisodeChanged != null && !mRxTopEpisodeChanged.isUnsubscribed()) {
            mRxTopEpisodeChanged.unsubscribe();
        }

        mRxTopEpisodeChanged = getEpisodeChangedSubscription();


        final String title = StrUtils.formatTitle(argEpisode.getTitle());
        final String description = argEpisode.getDescription();
        final int colorText = UIUtils.attrColor(R.attr.themeTextColorPrimary, mContext);
        View.OnClickListener onClickListener = getToast(title, description);
        SoundWaves soundwaves = SoundWaves.getAppContext(getContext());

        bindTitle(mEpisodeTitle, title, onClickListener, colorText);
        bindTitle(mEpisodeInfo, description.trim(), onClickListener, colorText);
        bindDuration(mTotalTime, argEpisode);
        setPlayerProgress(mCurrentTime, argEpisode);
        bindPlayPauseButton(mPlayPauseButton, argEpisode);
        bindSeekButton(mRewindButton, argEpisode, soundwaves, mOverlay, OnTouchSeekListener.BACKWARDS);
        bindSeekButton(mFastForwardButton, argEpisode, soundwaves, mOverlay, OnTouchSeekListener.FORWARD);

        if (argEpisode instanceof FeedItem) {
            mFavoriteButton.setFavorite(((FeedItem) argEpisode).isFavorite());
        }

        ISubscription iSubscription = argEpisode.getSubscription(getContext());
        if (mPlayer.isPlaying()) {
            setPlaybackSpeedView(mPlayer.getCurrentSpeedMultiplier());
        } else if (iSubscription instanceof org.bottiger.podcast.provider.Subscription) {
            org.bottiger.podcast.provider.Subscription subscription = (org.bottiger.podcast.provider.Subscription)iSubscription;
            setPlaybackSpeedView(subscription.getPlaybackSpeed());
        }

        bindSeekbar(mPlayerSeekbar, argEpisode, mOverlay);

        if (mPlayerDownloadButton != null) {
            mPlayerDownloadButton.setEpisode(argEpisode);
            mPlayerDownloadButton.enabledProgressListener(true);
        }

        String artworkURL = argEpisode.getArtwork(getContext());

        if (!TextUtils.isEmpty(iSubscription.getImageURL())) {
            artworkURL = iSubscription.getImageURL();
        }

        iSubscription.getColors(mContext)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {

                        @Override
                        public void onSuccess(ColorExtractor value) {
                            mPlayPauseButton.setColor(value);

                            int transparentgradientColor;
                            int gradientColor = value.getPrimary();

                            int alpha = 0;
                            int red = Color.red(gradientColor);
                            int green = Color.green(gradientColor);
                            int blue = Color.blue(gradientColor);
                            transparentgradientColor = Color.argb(alpha, red, green, blue);

                            GradientDrawable gd = new GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    new int[]{transparentgradientColor, gradientColor});
                            Drawable wrapDrawable = DrawableCompat.wrap(gd);
                            DrawableCompat.setTint(wrapDrawable, value.getPrimary());

                            mPlayerSeekbar.getProgressDrawable().setColorFilter(value.getPrimary(), PorterDuff.Mode.SRC_IN);
                        }
                    });


        Log.v("MissingImage", "Setting image");
        ImageLoaderUtils.loadImageInto(mPhoto, artworkURL, null, false, false, false, ImageLoaderUtils.DEFAULT);

        float speed = PlayerService.getPlaybackSpeed(getContext(), mCurrentEpisode);
        setPlaybackSpeedView(speed);

        // Set colors
        mTextColor = ColorUtils.getTextColor(getContext());
        int color = ColorUtils.getBackgroundColor(getContext());

        argEpisode.getSubscription(mContext).getColors(mContext)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {
                    @Override
                    public void onSuccess(ColorExtractor value) {
                        mPlayPauseButton.setColor(value);
                        mBackgroundColor = value.getPrimaryTint() != -1 ? value.getPrimaryTint() : ContextCompat.getColor(mContext, R.color.colorBgSecondary);

                        mBackgroundColor = ColorUtils.adjustToTheme(getResources(), mBackgroundColor);

                        if (mBackgroundColor != -1 && !UIUtils.isInNightMode(getResources())) {
                            setBackgroundColor(mBackgroundColor);
                        }

                        ColorUtils.tintButton(mFavoriteButton,    mTextColor);
                        tintInflatedButtons();

                        postInvalidate();
                    }
                });


        // Bind the car view
        if (mDrivingLayout != null) {
            assert mDrivingTitle != null;
            assert mDrivingSeekbar != null;
            assert mDrivingCurrentTime != null;
            assert mDrivingTotalTime != null;
            assert mDrivingPlayPause != null;
            assert mDrivingFastForward != null;
            assert mDrivingReverse != null;
            assert mDrivingPhoto != null;

            bindTitle(mDrivingTitle, title, onClickListener, colorText);
            bindSeekbar(mDrivingSeekbar, argEpisode, mOverlay);
            setPlayerProgress(mDrivingCurrentTime, argEpisode);
            bindDuration(mDrivingTotalTime, argEpisode);
            bindPlayPauseButton(mDrivingPlayPause, argEpisode);
            bindSeekButton(mDrivingReverse, argEpisode, soundwaves, mOverlay, OnTouchSeekListener.BACKWARDS);
            bindSeekButton(mDrivingFastForward, argEpisode, soundwaves, mOverlay, OnTouchSeekListener.FORWARD);
            ImageLoaderUtils.loadImageInto(mDrivingPhoto, artworkURL, null, false, false, false, ImageLoaderUtils.DEFAULT);
        }
    }

    public void togglePlayerType() {
        setPlayerType(mCurrentActivity == Constants.IN_VEHICLE ? OTHER : Constants.IN_VEHICLE);
    }

    public void setPlayerType(@Constants.Activities int argActivity) {
        if (mCurrentActivity == argActivity) {
            return;
        }

        mCurrentActivity = argActivity;


        if (mDrivingScene == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            mDrivingLayout = inflater.inflate(R.layout.playlist_top_player_driving,
                    mControls, false);

            mDrivingTitle = (TextView) mDrivingLayout.findViewById(R.id.player_title);
            mDrivingSeekbar = (PlayerSeekbar) mDrivingLayout.findViewById(R.id.top_player_seekbar);
            mDrivingCurrentTime = (TextViewObserver) mDrivingLayout.findViewById(R.id.current_time);
            mDrivingTotalTime = (TextView) mDrivingLayout.findViewById(R.id.total_time);
            mDrivingPlayPause = (PlayPauseImageView) mDrivingLayout.findViewById(R.id.playpause);
            mDrivingFastForward = (ImageView) mDrivingLayout.findViewById(R.id.top_player_fastforward);
            mDrivingReverse = (ImageView) mDrivingLayout.findViewById(R.id.top_player_rewind);
            mDrivingPhoto = (ImageView) mDrivingLayout.findViewById(R.id.session_photo);

            bind(mCurrentEpisode);

            mDrivingScene = new Scene(mControls, mDrivingLayout);
        }

        Scene newScene = mCurrentActivity == Constants.IN_VEHICLE ? mDrivingScene : mPrimaryScene;
        TransitionManager.go(newScene);
    }

    private void openTimePicker() {

        Calendar rightNow = Calendar.getInstance();

        final int hourStart = rightNow.get(Calendar.HOUR_OF_DAY);
        final int minuteStart = rightNow.get(Calendar.MINUTE);

        if (mCountDownTimeLeft > TIMER_NOT_SET) {
            rightNow.add(Calendar.MILLISECOND, (int)mCountDownTimeLeft);
        } else {
            rightNow.add(Calendar.MINUTE, DEFAULT_SLEEP_TIME_MIN);
        }

        int hour_sleep = rightNow.get(Calendar.HOUR);
        int minute_sleep = rightNow.get(Calendar.MINUTE);

        TimePickerDialog tpd = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePickerDialog view, int setHourOfDay, int setMinute, int setSecond) {
                int hourDiff = setHourOfDay >= hourStart ? setHourOfDay - hourStart : setHourOfDay+24 - hourStart;
                int minuteDiff = setMinute - minuteStart;

                int setMinutes = hourDiff*60 + minuteDiff;

                if (setMinutes > 0) {
                    sleepButtonPressed(setMinutes);
                }
            }
        }, hour_sleep, minute_sleep, false);

        tpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                sleepButtonPressed(-1);
            }
        });

        tpd.setCancelText(R.string.player_sleep_dialog_cancel_button);
        tpd.setOkText(R.string.player_sleep_dialog_ok_button);

        if (getContext() instanceof Activity) {
            Activity activity = (Activity) getContext();
            tpd.show(activity.getFragmentManager(), "TimePickerDialog");
        }
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    float getPlayerHeight() {
        return screenHeight;
    }

    public void setPlaybackSpeedView(float argSpeed) {
        if (mSpeedButton == null)
            return;

        mSpeedButton.setText(PlaybackSpeed.toString(argSpeed));
    }

    synchronized float scrollBy(float argY) {
        float oldHeight = getPlayerHeight();
        return setPlayerHeight(oldHeight - argY);
    }

    public void setPlaylistEmpty(boolean argDoFill) {
        Log.v(TAG, "Player height fill: " + argDoFill);

        if (argDoFill == mPlaylistEmpty)
            return;

        mPlaylistEmpty = argDoFill;
    }

    private float canConsume(float argDiff) {
        float currentHeight = getPlayerHeight();

        if (currentHeight <= sizeSmall) {
            return 0;
        }

        if (currentHeight >= sizeLarge) {
            return 0;
        }

        float newHeight = currentHeight+argDiff;

        if (argDiff > 0) {
            return newHeight-sizeLarge;
        }

        return newHeight-sizeSmall;
    }

    // returns the new height
    private float setPlayerHeight(float argScreenHeight) {
        Log.v(TAG, "Player height is set to: " + argScreenHeight);

        if (mPlaylistEmpty)
            return getHeight();

        if (!validateState()) {
            return -1;
        }

        Log.v("TopPlayer", "setPlayerHeight: " + argScreenHeight);

        screenHeight = argScreenHeight;

        setBackgroundVisibility(screenHeight);

        if (mPlaylistUpArrow != null) {
            setGenericVisibility(mPlaylistUpArrow, getMaximumSize(), 100, screenHeight);
        }

        float minScreenHeight = screenHeight < sizeSmall ? sizeSmall : screenHeight;
        float minMaxScreenHeight = minScreenHeight > sizeLarge ? sizeLarge : minScreenHeight;

        float controlTransY = minMaxScreenHeight - (mPlayPauseButton.getBottom() + 40); //525 minMaxScreenHeight-mControlHeight;

        if (controlTransY >= 0) {
            controlTransY = 0;
        }

        screenHeight = minMaxScreenHeight;

        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = (int)minMaxScreenHeight;
        setLayoutParams(lp);

        translateControls(controlTransY);

        return minMaxScreenHeight;

    }

    private void translateControls(float argTranslationY) {
        Log.v("TopPlayer", "controlTransY: " +  argTranslationY);
        mEpisodeTitle.setTranslationY(argTranslationY);
        mPlayerSeekbar.setTranslationY(argTranslationY);
        mCurrentTime.setTranslationY(argTranslationY);
        mTotalTime.setTranslationY(argTranslationY);
        mPlayPauseButton.setTranslationY(argTranslationY);
        mFastForwardButton.setTranslationY(argTranslationY);
        mRewindButton.setTranslationY(argTranslationY);
        mMoreButton.setTranslationY(argTranslationY);

        mTotalTime.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY);
        mTotalTime.requestLayout();
    }

    public float setBackgroundVisibility(float argTopPlayerHeight) {
        return setGenericVisibility(mPhoto, getMaximumSize(), 300, argTopPlayerHeight);
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

        float scaleFraction = 1f - (1f - VisibilityFraction)/10;
        argView.setScaleX(scaleFraction);
        argView.setScaleY(scaleFraction);

        if (VisibilityFraction > 0f) {
            float newHeight = argVisibleHeight - argVisibleHeight * scaleFraction;
            float translationY = newHeight / 2;
            argView.setTranslationY(-translationY);
        }

        argView.setAlpha(VisibilityFraction);

        return VisibilityFraction;
    }

    public int getVisibleHeight() {
        return (int) (getHeight()+getTranslationY());
    }

    private boolean validateState() {
        if (sizeSmall < 0 || sizeMedium < 0 || sizeLarge < 0) {
            Log.d("TopPlayer", "mMainLayout sizes needs to be defined");
            return false;
        }
        return true;
    }

    public boolean isMaximumSize() {
        return getHeight() >= sizeLarge;
    }

    public boolean isMinimumSize() {
        return sizeSmall >= getHeight()+getTranslationY();
    }

    public int getMaximumSize() {
        return sizeLarge;
    }

    public int getMinimumSize() {
        return sizeSmall;
    }

    private void setTextVIsibility(int argVisibility) {
        mTriangle.setVisibility(argVisibility);
        mExpandedActionsBar.setVisibility(argVisibility);
    }

    private void getPlayerControlHeight(@NonNull final View argView) {
        ViewTreeObserver viewTreeObserver = argView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    UIUtils.removeOnGlobalLayoutListener(argView, this);
                    int controlHeight = argView.getHeight();
                    if (controlHeight > 0) {
                        mControlHeight = controlHeight;
                        argView.getLayoutParams().height = mControlHeight;
                    }
                }
            });
        }
    }

    public @ColorInt int getBackGroundColor() {
        return mBackgroundColor;
    }

    public synchronized void setFullscreen(boolean argNewState, boolean doAnimate) {
        mFullscreen = argNewState;
        try {
            if (doAnimate) {
                Transition trans = new ChangeBounds();
                trans.setDuration(getResources().getInteger(R.integer.animation_quick));
                TransitionManager.go(new Scene(this), trans);
            }

            if (mFullscreen) {
                goFullscreen();
            } else {
                exitFullscreen();
            }

            setFullscreenImagePadding(getContext());
        } finally {
            prefs.edit().putBoolean(mDoFullscreentKey, argNewState).apply();
        }
    }

    private void goFullscreen() {
        Log.d(TAG, "Enter fullscreen mode");

        if (mMainLayout != null) {

            setPlayerHeight(sizeLarge);

            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);

            mMainLayout.setLayoutParams(layoutParams);
        }
    }

    private void exitFullscreen() {

        Log.d(TAG, "Exit fullscreen mode");
        //mFullscreenButton.setImageResource(R.drawable.ic_fullscreen_white);

        if (mMainLayout != null) {
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    sizeLarge);

            mMainLayout.setLayoutParams(layoutParams);

            setPlayerHeight(sizeLarge);
        }
    }

    public boolean isFullscreen() {
        return mFullscreen;
    }

    private synchronized void onClickMoreButton(@NonNull View argView) {

        if (mMoreButtonsStub != null) {
            View inflated = mMoreButtonsStub.inflate();
            mMoreButtonsStub = null;

            mFullscreenButton = (PlayerButtonView) inflated.findViewById(R.id.fullscreen_button);
            mSleepButton = (Button) inflated.findViewById(R.id.sleep_button);
            mChapterButton = (ImageView) inflated.findViewById(R.id.chapter_button);
            mSpeedButton = (Button) inflated.findViewById(R.id.speed_button);
            mExpandedActionsBar = (LinearLayout) inflated.findViewById(R.id.expanded_action_bar);
            mPlayerDownloadButton = (DownloadButtonView) findViewById(R.id.download);

            initExpandedButtons();
        }

        try {
            mDoDisplayText = !mDoDisplayText;
            TransitionManager.beginDelayedTransition(mMainLayout, UIUtils.getDefaultTransition(getResources()));

            if (mDoDisplayText) {
                Log.d(TAG, "ShowText");
                setTextVIsibility(VISIBLE);
            } else {
                Log.d(TAG, "HideText");
                setTextVIsibility(GONE);
            }
        } finally {
            prefs.edit().putBoolean(mDoDisplayTextKey, mDoDisplayText).apply();
        }
    }

    private void tintInflatedButtons() {
        if (mSpeedButton != null) {
            ColorUtils.tintButton(mSpeedButton, mTextColor);
        }

        if (mFullscreenButton != null) {
            ColorUtils.tintButton(mFullscreenButton, mTextColor);
        }

        if (mPlayerDownloadButton != null) {
            ColorUtils.tintButton(mPlayerDownloadButton, mTextColor);
        }
    }

    private void initExpandedButtons() {
        assert mSleepButton != null;
        assert mChapterButton != null;
        assert mSpeedButton != null;
        assert mFullscreenButton != null;
        assert mPlayerDownloadButton != null;

        mPlayerDownloadButton.setEpisode(mCurrentEpisode);
        mPlayerDownloadButton.enabledProgressListener(true);

        tintInflatedButtons();

        // Sleep buttons
        mSleepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCountDownTimeLeft > TIMER_NOT_SET) {
                    openTimePicker();
                } else {
                    sleepButtonPressed();
                }
            }
        });

        GenericMediaPlayerInterface player = SoundWaves.getAppContext(mContext).getPlayer();
        final long countDownMs = player.timeUntilFadeout();
        setCountDownText(countDownMs);

        // Chapter button
        mChapterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                IEpisode episode = mCurrentEpisode;
                if (!(context instanceof MainActivity) || episode == null)
                    return;

                MainActivity activity = (MainActivity)context;
                DialogChapters dialogChapters = DialogChapters.newInstance(episode);
                dialogChapters.show(activity.getFragmentManager(), DialogPlaybackSpeed.class.getName());
            }
        });

        int visibility = SoundWaves.getAppContext(getContext()).getPlayer().canSetSpeed() ? View.VISIBLE : View.GONE;
        mSpeedButton.setVisibility(visibility);

        float speedMultiplier = player.getCurrentSpeedMultiplier();
        setPlaybackSpeedView(speedMultiplier);

        mSpeedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = ((MainActivity)getContext());
                DialogPlaybackSpeed dialogPlaybackSpeed = DialogPlaybackSpeed.newInstance(DialogPlaybackSpeed.EPISODE);
                dialogPlaybackSpeed.show(activity.getFragmentManager(), DialogPlaybackSpeed.class.getName());
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
    }

    private void setCountDownText(final long argDuration) {
        if (argDuration > 0) {
            mCountDownTimer = new CountDownTimer(argDuration, 1000) {

                public void onTick(long millisUntilFinished) {
                    mCountDownTimeLeft = millisUntilFinished;
                    if (mSleepButton != null) {
                        GenericMediaPlayerInterface player = SoundWaves.getAppContext(mContext).getPlayer();
                        mSleepButton.setText(StrUtils.formatTime(player.timeUntilFadeout()));
                    }
                }

                public void onFinish() {
                    mCountDownTimeLeft = TIMER_NOT_SET;
                    if (mSleepButton != null)
                        mSleepButton.setText("");
                }
            }.start();
        }
    }

    private void setFullscreenImagePadding(@NonNull Context argContext) {
        int padding = (int)argContext.getResources().getDimension(R.dimen.player_fullscreen_image_padding);
        mPhoto.setPadding(padding, 0 ,padding, padding);
    }

    private View.OnClickListener getToast(@NonNull final String argTitle, @NonNull final String argDescription) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View argView) {
                String msg = argTitle + "\n\n" + argDescription;
                // HACK: ugly, but it does actually work :)
                // double the lifetime of a toast
                for (int i = 0; i < 2; i++) {
                    Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        };
    }

    public void onDestroyView() {

        if (mRxPlayerChanged != null && !mRxPlayerChanged.isUnsubscribed()) {
            mRxPlayerChanged.unsubscribe();
        }

        if (mRxTopEpisodeChanged != null && !mRxTopEpisodeChanged.isUnsubscribed()) {
            mRxTopEpisodeChanged.unsubscribe();
        }

        unsetPlayer();

    }

    private static void setPlayerProgress(@NonNull TextViewObserver argCurrentTime, @NonNull IEpisode argEpisode) {
        long offset = argEpisode.getOffset();
        if (offset > 0) {
            argCurrentTime.setText(StrUtils.formatTime(offset));
        } else {
            argCurrentTime.setText("00:00");
        }
        argCurrentTime.setEpisode(argEpisode);
    }

    private void setPlayer() {
        unsetPlayer();

        mPlayer = SoundWaves.getAppContext(getContext()).getPlayer();

        mPlayer.addListener(mPlayerSeekbar);
        mPlayer.addListener(mCurrentTime);
        mPlayer.addListener(mPlayPauseButton);
    }

    private void unsetPlayer() {
        if (mPlayer != null) {
            mPlayer.removeListener(mPlayerSeekbar);
            mPlayer.removeListener(mCurrentTime);
            mPlayer.removeListener(mPlayPauseButton);
        }
    }

    private static void bindSeekButton(@NonNull ImageView argSeekButton,
                                       @NonNull IEpisode argEpisode,
                                       @NonNull SoundWaves argApp,
                                       @NonNull Overlay argOverlay,
                                       @OnTouchSeekListener.Direction int argDirection) {
        argSeekButton.setOnTouchListener(OnTouchSeekListener.getSeekListener(argApp,
                argEpisode,
                argDirection,
                argOverlay));
    }

    private static void bindPlayPauseButton(@Nullable PlayPauseImageView argPlayPauseImageView, @NonNull IEpisode argEpisode) {
        argPlayPauseImageView.setEpisode(argEpisode, PlayPauseImageView.PLAYLIST);
        argPlayPauseImageView.setStatus(STATE_READY);
    }

    private static void bindDuration(@Nullable TextView argDurationView, @NonNull IEpisode argEpisode) {
        if (argDurationView == null) {
            return;
        }

        long duration = argEpisode.getDuration();
        if (duration > 0) {
            argDurationView.setText(StrUtils.formatTime(duration));
        } else {
            PlayerHelper.setDuration(argEpisode, argDurationView);
        }
    }

    private static void bindTitle(@Nullable TextView argTitleView,
                                  @NonNull String argTitle,
                                  @NonNull View.OnClickListener argOnClickListener,
                                  @NonNull @ColorInt int argColor) {
        if (argTitleView == null)
            return;

        argTitleView.setText(argTitle);
        argTitleView.setOnClickListener(argOnClickListener);
        argTitleView.setTextColor(argColor);
    }

    private static void bindSeekbar(@NonNull PlayerSeekbar argSeekbar, @NonNull IEpisode argEpisode, @NonNull Overlay argOverlay) {
        argSeekbar.setEpisode(argEpisode);
        argSeekbar.setOverlay(argOverlay);
        argSeekbar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
    }

    private Subscription getPlayerSubscription() {
        return SoundWaves
                .getRxBus()
                .toObserverable()
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .ofType(NewPlayerEvent.class)
                .subscribe(new Action1<NewPlayerEvent>() {
                    @Override
                    public void call(NewPlayerEvent playlistChanged) {
                        Log.wtf(TAG, "NewPlayerEvent: NewPlayerEvent event recieved");
                        setPlayer();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.wtf(TAG, "ERROR: NewPlayerEvent: mRxPlaylistSubscription event recieved");
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.wtf(TAG, "error: " + throwable.toString());
                    }
                });
    }

    private Subscription getEpisodeChangedSubscription() {
        return SoundWaves.getRxBus()
                .toObserverable()
                .onBackpressureDrop()
                .ofType(EpisodeChanged.class)
                .filter(new Func1<EpisodeChanged, Boolean>() {
                    @Override
                    public Boolean call(EpisodeChanged episodeChanged) {
                        return episodeChanged.getAction() != EpisodeChanged.PLAYING_PROGRESS &&
                                episodeChanged.getAction() != EpisodeChanged.DOWNLOAD_PROGRESS;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<EpisodeChanged>() {
                    @Override
                    public void call(EpisodeChanged itemChangedEvent) {
                        long episodeId = itemChangedEvent.getId();
                        IEpisode episode = SoundWaves.getAppContext(getContext()).getLibraryInstance().getEpisode(episodeId);

                        if (episode == null)
                            return;

                        if (episode.equals(mCurrentEpisode)) {
                            TopPlayer.this.bind(episode);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.handleException(throwable);
                        Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                    }
                });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.w(TAG, "touchdebug intercept : " + event.getAction());
        return super.onInterceptTouchEvent(event);
    }

    public boolean scrollExternal(float distanceY, boolean argDispatchScrollEvents) {

        float diffY = distanceY;
        int dyConsumed = 0;

        float diffYabs = Math.abs(diffY);
        int dyUnconsumed = (int)diffY;

        if (argDispatchScrollEvents)
            scrollingChildHelper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);


        int[] offsetInWindow = new int[] {0, 0};

        int[] consumed = new int[] { 0, 0};


        if (argDispatchScrollEvents)
            scrollingChildHelper.dispatchNestedPreScroll(0, dyUnconsumed, consumed, offsetInWindow);


        //diffY = diffY + offsetInWindow[1];
        dyUnconsumed += offsetInWindow[1];
        offsetInWindow[1] = 0;


        int dyCanConsume = (int)canConsume(dyUnconsumed);
        dyConsumed = dyCanConsume > Math.abs(diffY) ? dyUnconsumed : dyCanConsume;

        dyUnconsumed = Math.abs(diffY) > Math.abs(dyConsumed) ? (dyUnconsumed - dyConsumed) : 0;


        if (argDispatchScrollEvents)
            scrollingChildHelper.dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, offsetInWindow );


        int treshold = TopPlayer.this.mViewConfiguration.getScaledTouchSlop();
        if (diffYabs > 0) {//(diffYabs > ViewConfigurationCompat.getScaledPagingTouchSlop(TopPlayer.this.mViewConfiguration)) {
            Log.d(TAG, "onScroll: dispatchScroll: dyUnconsumed: " + dyConsumed);

            int consumedByActionbar = consumed[1];

            // if we are pulling down, but do not consume all the pulls
            if (dyConsumed < 0) {
                TopPlayer.this.scrollBy(dyConsumed-consumedByActionbar);
            }

            if (dyConsumed > 0 && !TopPlayer.this.isMinimumSize()) {
                TopPlayer.this.scrollBy(dyConsumed-consumedByActionbar);
            }
        }


        Log.d(TAG, "onScroll: ignore: diffY: " + diffY);
        return true;
    }

    class TopPLayerScrollGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final String DEBUG_TAG = "Gestures";

        private Runnable mFlingRunnable;

        public TopPLayerScrollGestureListener() {
        }

        @Override
        public  boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(DEBUG_TAG, "onScroll: e1Y: " + e1.getY() + " e2Y: " + e2.getY());
            return scrollExternal(distanceY, true);
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());


            if(this.mFlingRunnable != null) {
                removeCallbacks(this.mFlingRunnable);
            }

            if(TopPlayer.this.mScroller == null) {
                TopPlayer.this.mScroller = ScrollerCompat.create(getContext());
            }

            scrollingChildHelper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

            TopPlayer.this.mScroller.fling(0, PlaylistBehavior.MAX_VELOCITY_Y, 0, Math.round(velocityY), 0, 0, 0, 10000);
            if(TopPlayer.this.mScroller.computeScrollOffset()) {
                this.mFlingRunnable = new FlingRunnable(TopPlayer.this);
                ViewCompat.postOnAnimation(TopPlayer.this, this.mFlingRunnable);
                return true;
            } else {
                this.mFlingRunnable = null;
                return false;
            }
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
        return 0;// this.mMainLayout.canScrollHorizontally()?this.mMainLayout.computeHorizontalScrollOffset(this.mState):0;
    }

    // Compute the horizontal offset of the horizontal scrollbar's thumb within the horizontal range.
    public int computeHorizontalScrollExtent() {
        return 0;// this.mMainLayout.canScrollHorizontally()?this.mMainLayout.computeHorizontalScrollExtent(this.mState):0;
    }

    // Compute the horizontal range that the horizontal scrollbar represents.
    public int computeHorizontalScrollRange() {
        return 0;// this.mMainLayout.canScrollHorizontally()?this.mMainLayout.computeHorizontalScrollRange(this.mState):0;
    }

    // Compute the vertical extent of the vertical scrollbar's thumb within the vertical range.
    public int computeVerticalScrollOffset() {
        return 0;// this.mMainLayout.canScrollVertically()?this.mMainLayout.computeVerticalScrollOffset(this.mState):0;
    }

    // Compute the vertical offset of the vertical scrollbar's thumb within the horizontal range.
    public int computeVerticalScrollExtent() {
        return 0;// this.mMainLayout.canScrollVertically()?this.mMainLayout.computeVerticalScrollExtent(this.mState):0;
    }

    // Compute the vertical range that the vertical scrollbar represents.
    public int computeVerticalScrollRange() {
        return 0;// this.mMainLayout.canScrollVertically()?this.mMainLayout.computeVerticalScrollRange(this.mState):0;
    }


    class FlingRunnable implements Runnable {
        private final View mLayout;

        private int lastY = 0;

        FlingRunnable(View layout) {
            this.mLayout = layout;
        }

        public void run() {
            if(TopPlayer.this.mScroller != null && TopPlayer.this.mScroller.computeScrollOffset()) {

                int currY = TopPlayer.this.mScroller.getCurrY();

                if (lastY != 0) {
                    int diffY = currY-lastY;

                    Log.v(TAG, "scroll.run, diffY: " + diffY);
                    TopPlayer.this.scrollExternal(-diffY, false);
                }

                lastY = currY;
                ViewCompat.postOnAnimation(this.mLayout, this);
            }

        }
    }


}
