package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.utils.StrUtils;

/**
 * Created by aplb on 02-12-2016.
 */

public class Overlay extends FrameLayout {

    private static final String TAG = Overlay.class.getSimpleName();

    private static final int DEFAULT_DISPLAY_LENGTH_MS = 1_000;

    @NonNull private TextView mBackwards;
    @NonNull private TextView mCurrent;
    @NonNull private TextView mForwards;

    @NonNull private StringBuilder mStringBuilder = new StringBuilder();

    @NonNull private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Overlay.this.setVisibility(GONE);
        }
    };

    public Overlay(Context context) {
        super(context);
        init();
    }

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Overlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Overlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mBackwards = (TextView) findViewById(R.id.seekbar_time_backwards);
        mCurrent =   (TextView) findViewById(R.id.seekbar_time_current);
        mForwards =  (TextView) findViewById(R.id.seekbar_time_forward);
    }

    private void init() {
    }

    public void show() {
        show(DEFAULT_DISPLAY_LENGTH_MS);
    }

    public void show(int argDuration) {
        setVisibility(VISIBLE);
        removeCallbacks(mRunnable);
        postDelayed(mRunnable, argDuration);
    }

    public void setText(long argCurrentTimeMs, long argOffsetMs) {
        mStringBuilder.setLength(0);
        boolean positiveOffset = argOffsetMs >= 0;

        mStringBuilder.append(positiveOffset ? "+" : "-");
        mStringBuilder.append(StrUtils.formatTime(argOffsetMs));

        TextView current = mCurrent;
        TextView forward = mForwards;
        TextView backward = mBackwards;

        if (current == null || forward == null || backward == null) {
            VendorCrashReporter.report(TAG, "View is null");
            return;
        }

        current.setText(StrUtils.formatTime(argCurrentTimeMs));
        forward.setText(positiveOffset ? mStringBuilder.toString() : "");
        backward.setText(positiveOffset ? "" : mStringBuilder.toString());
    }

    @NonNull
    public TextView getCurrent() {
        return mCurrent;
    }

    @NonNull
    public TextView getForward() {
        return mForwards;
    }

    @NonNull
    public TextView getBackwards() {
        return mBackwards;
    }
}
