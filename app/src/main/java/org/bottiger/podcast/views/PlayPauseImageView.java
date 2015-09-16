package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;

import com.squareup.otto.Subscribe;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusData;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusProgressData;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.views.dialogs.DialogOpenVideoExternally;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TODO: document your custom view class.
 */
public class PlayPauseImageView extends ImageButton implements PaletteListener,
                                                             DownloadObserver,
                                                             View.OnClickListener {

    private static final String TAG = "PlayPauseImageView";

    private static final boolean DRAW_PROGRESS          = true;
    private static final boolean DRAW_PROGRESS_MARKER   = true;

    private static final String MIME_VIDEO = "video/*";

    //public enum LOCATION { PLAYLIST, FEEDVIEW, DISCOVERY_FEEDVIEW, OTHER };
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PLAYLIST, FEEDVIEW, DISCOVERY_FEEDVIEW, OTHER})
    public @interface ButtonLocation {}
    public static final int PLAYLIST = 1;
    public static final int FEEDVIEW = 2;
    public static final int DISCOVERY_FEEDVIEW = 3;
    public static final int OTHER = 4;

    private @ButtonLocation int mLocation = OTHER;

    private static final int START_ANGLE = -90;
    private static final int DRAW_OFFSET = 6;
    private static final int DRAW_WIDTH = 6;

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;

    private IEpisode mEpisode;

    protected Context mContext;

    private RectF bounds = new RectF();
    private Rect boundsRound = new Rect();

    private boolean mDrawBackground;
    private int mProgressPercent = 0;

    protected Paint paint;
    private Paint paintBorder;

    private int mPaintColor = getResources().getColor(R.color.colorPrimaryDark);
    private int mPaintBorderColor = Color.WHITE;

    public PlayPauseImageView(Context context) {
        super(context);
        init(context);
    }

    public PlayPauseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        initAttr(attrs);
    }

    public PlayPauseImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
        initAttr(attrs);
    }

    private void init(Context argContext) {

        mContext = argContext;

        paint = new Paint(Paint.LINEAR_TEXT_FLAG);
        paint.setColor(mPaintColor);
        paint.setTextSize(12.0F);
        paint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        paint.setStrokeWidth(1F);

        paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setColor(mPaintBorderColor);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(DRAW_WIDTH);

        setScaleType(ScaleType.CENTER);

        if (Build.VERSION.SDK_INT >= 16) {
            setBackground(null);
        }

        if (isInEditMode()) {
            return;
        }

        setOnClickListener(this);
    }

    private void initAttr(AttributeSet attrs) {
        TypedArray a = mContext.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PlayPauseImageView,
                0, 0);

        try {
            mDrawBackground = a.getBoolean(R.styleable.PlayPauseImageView_drawBackground, true);
        } finally {
            a.recycle();
        }

    }

    @NonNull
    @Override
    public IEpisode getEpisode() {
        return mEpisode;
    }

    public synchronized void setEpisode(IEpisode argEpisode, @ButtonLocation int argLocation) {
        this.mLocation = argLocation;
        this.mEpisode = argEpisode;


        long offset = mEpisode instanceof FeedItem && ((FeedItem)mEpisode).offset > 0 ? ((FeedItem)mEpisode).offset : 0;
        setProgressMs(new PlayerStatusProgressData(offset));
        invalidate();
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisode = null;
    }

    public void setStatus(PlayerStatusObservable.STATUS argStatus) {
        mStatus = argStatus;

        int resid;
        if (mStatus == PlayerStatusObservable.STATUS.PLAYING) {
            resid = drawBackground() ? R.drawable.ic_pause_white : R.drawable.ic_pause_black;
        } else {
            resid = drawBackground() ? R.drawable.ic_play_arrow_white : R.drawable.ic_play_arrow_black;
        }
        setImageResource(resid);

        this.invalidate();
    }

    public void setColor(@ColorInt int argColor, @ColorInt int argOuterColor) {
        float scale = 1.3f;
        float red = Color.red(argColor)*scale;
        float green = Color.green(argColor)*scale;
        float blue = Color.blue(argColor)*scale;

        int newColor = Color.argb(255, (int)red, (int)green, (int)blue);

        float darkPrimary = newColor;
        paint.setColor((int)darkPrimary);
        paintBorder.setColor(argOuterColor);
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int contentWidth = getWidth();
        int contentHeight = getHeight();

        int centerX = contentWidth/2;
        int centerY = contentHeight/2;

        // Draw the background circle
        float radius = centerX-DRAW_WIDTH;

        if (drawBackground()) {
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        int diff2 =  DRAW_WIDTH;//(int) (centerY-radius);
        boolean updateOutline = bounds == null;

        bounds.left =DRAW_OFFSET;
        bounds.top = diff2;
        bounds.right = contentWidth - DRAW_OFFSET;
        bounds.bottom = contentWidth - diff2;

        if (updateOutline) {
            onSizeChanged(0, 0, 0, 0);
        }

        if (DRAW_PROGRESS && getEpisode() != null && getEpisode().isMarkedAsListened()) {
            canvas.drawCircle(centerX, centerY, radius, paintBorder);
        } else if (DRAW_PROGRESS && mProgressPercent > 0) {
            canvas.drawArc(bounds, START_ANGLE, getProgressAngle(mProgressPercent), false, paintBorder);
        }

        super.onDraw(canvas);
    }

    @Subscribe
    public void setProgressMs(PlayerStatusProgressData argPlayerProgress) {

        // copy from seekbar

        if (argPlayerProgress.progressMs < 0) {
            throw new IllegalStateException("Progress must be positive");
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
            progress = argPlayerProgress.progressMs / duration * 100;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgressPercent((int) progress);
    }

    @Subscribe
    public void onPlayerStateChange(PlayerStatusData argPlayerStatus) {
        if (argPlayerStatus == null)
            return;

        if (!getEpisode().equals(argPlayerStatus.episode)) {

            setStatus(PlayerStatusObservable.STATUS.PAUSED);
            return;
        }

        setStatus(argPlayerStatus.status);
    }

    public void onStateChange(EpisodeStatus argStatus) {
        if (!argStatus.getEpisode().equals(mEpisode)) {
            return;
        }

        if (mStatus == argStatus.getStatus()) {
            return;
        }

        setStatus(argStatus.getStatus());
    }

    @Override
    public void setProgressPercent(int argProgress) {
        mProgressPercent = argProgress;
        invalidate();
    }

    @Override
    public void onClick(View view) {

        IAnalytics.EVENT_TYPE type = getEventType();
        if (type != null) {
            SoundWaves.sAnalytics.trackEvent(type);
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
                    prefs.edit().putBoolean(hasAskedKey, true).commit();
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

        boolean isPlaying = SoundWaves.sBoundPlayerService.toggle(mEpisode);
        setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.STOPPED);

        //SoundWaves.getBus().post(new PlaylistData().playlistChanged = true);
    }

    public static void openVideoExternally(@NonNull IEpisode argEpisode, @NonNull Context argContext) {
        Uri uri = argEpisode.getFileLocation(IEpisode.PREFER_LOCAL);

        String mimetype;
        if (argEpisode.isDownloaded()) {
            mimetype = EpisodeDownloadManager.getMimeType(argEpisode.getFileLocation(IEpisode.REQUIRE_LOCAL).toString());
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(argEpisode.getFileLocation(IEpisode.REQUIRE_REMOTE).toString());
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, mimetype);
        //argContext.startActivity(Intent.createChooser(intent, argContext.getString(R.string.choose_player_for_open_video_externally)));
        argContext.startActivity(intent);
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(mContext, argChangedPalette);
        setColor(extractor.getPrimary(), extractor.getSecondary());
        //setColor(extractor.getSecondary(), extractor.getSecondaryTint());
    }

    @Override
    public String getPaletteUrl() {
        return mEpisode.getUrl().toString();
    }

    private static int smallSize = -1;
    private static int largeSize = -1;

    public static int getSmallSize(@NonNull Context argContext) {
        if (smallSize > 0)
            return smallSize;

        smallSize = argContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size_normal);

        return smallSize;
    }

    public static int getLargeSize(@NonNull Context argContext) {
        if (largeSize > 0)
            return largeSize;

        largeSize = argContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size);

        return largeSize;
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

    protected boolean drawIcon() {
        return true;
    }

    protected boolean drawBackground() {
        return mDrawBackground;
    }
}
